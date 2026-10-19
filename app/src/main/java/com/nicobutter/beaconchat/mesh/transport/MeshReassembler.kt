package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.core.MeshClock
import java.util.UUID

/** Opaque local connection incarnation, recreated on reconnect; NOT a permanent node identity. */
data class MeshLinkSession(val value: UUID)

data class ReassemblyLimits(
    val maxConcurrentTransfers: Int = 8,
    val maxTransfersPerSession: Int = 2,
    /** Includes completed/aborted tombstones; a full table rejects new transfers. */
    val maxTrackedTransfers: Int = 32,
    /** Reserve full advertised lengths at admission, not just bytes already received. */
    val maxBufferedBytes: Int = 8 * MeshFragmentProtocol.MAX_TRANSFER_BYTES,
    val maxTransferBytes: Int = MeshFragmentProtocol.MAX_TRANSFER_BYTES,
    val maxFragments: Int = MeshFragmentProtocol.MAX_FRAGMENTS,
    val retentionMillis: Long = 15_000
) {
    init {
        require(maxConcurrentTransfers > 0 && maxTransfersPerSession in 1..maxConcurrentTransfers)
        require(maxTrackedTransfers >= maxConcurrentTransfers && maxBufferedBytes > 0)
        require(maxTransferBytes in 1..MeshFragmentProtocol.MAX_TRANSFER_BYTES)
        require(maxFragments in 1..MeshFragmentProtocol.MAX_FRAGMENTS && retentionMillis > 0)
    }
}

enum class FragmentRejection {
    MALFORMED_FRAME, LIMIT_EXCEEDED, CAPACITY, INCONSISTENT_TRANSFER,
    CONFLICTING_DUPLICATE, INTEGRITY_FAILED, CLOSED_TRANSFER
}

sealed class ReassemblyResult {
    object Pending : ReassemblyResult()
    object Duplicate : ReassemblyResult()
    data class Complete(val payload: ByteArray) : ReassemblyResult()
    data class Rejected(val reason: FragmentRejection) : ReassemblyResult()
}

data class ReassemblySnapshot(val activeTransfers: Int, val trackedTransfers: Int, val bufferedBytes: Int)

/**
 * Hardware-free, synchronous and caller-serialized (not thread safe). No timers or automatic retries.
 * A deadline starts at the first fragment, never slides, and includes terminal tombstone retention.
 * Only Complete may be decoded and passed to MeshEngine. Incomplete buffers expire without delivery.
 * Host supplies monotonic time, calls cleanup while idle, and routes ONLY the dedicated mesh channel.
 */
class MeshReassembler(private val clock: MeshClock, private val limits: ReassemblyLimits = ReassemblyLimits()) {
    private data class Key(val session: MeshLinkSession, val transferId: Int)
    private sealed class Transfer(val expiresAt: Long)
    private class Active(val header: MeshFragmentProtocol.Header, expiresAt: Long) : Transfer(expiresAt) {
        val bytes = ByteArray(header.totalBytes)
        val received = BooleanArray(header.count)
        var receivedCount = 0
    }
    private class Closed(expiresAt: Long) : Transfer(expiresAt)

    private val transfers = LinkedHashMap<Key, Transfer>()
    private var activeTransfers = 0
    private var bufferedBytes = 0
    private var lastTime = Long.MIN_VALUE

    fun accept(session: MeshLinkSession, frame: ByteArray): ReassemblyResult {
        val now = now()
        cleanupAt(now)
        val header = try { MeshFragmentProtocol.parse(frame) } catch (_: IllegalArgumentException) {
            return rejected(FragmentRejection.MALFORMED_FRAME)
        }
        val key = Key(session, header.transferId)
        val existing = transfers[key]
        if (existing is Closed) return rejected(FragmentRejection.CLOSED_TRANSFER)
        if (existing is Active && (existing.header.totalBytes != header.totalBytes ||
                existing.header.count != header.count || existing.header.checksum != header.checksum)) {
            close(key, existing)
            return rejected(FragmentRejection.INCONSISTENT_TRANSFER)
        }
        if (header.totalBytes > limits.maxTransferBytes || header.count > limits.maxFragments) {
            return rejected(FragmentRejection.LIMIT_EXCEEDED)
        }
        val active = existing as? Active ?: run {
            if (activeTransfers >= limits.maxConcurrentTransfers || transfers.size >= limits.maxTrackedTransfers ||
                header.totalBytes > limits.maxBufferedBytes - bufferedBytes ||
                transfers.count { it.key.session == session && it.value is Active } >= limits.maxTransfersPerSession) {
                return rejected(FragmentRejection.CAPACITY)
            }
            val deadline = if (now > Long.MAX_VALUE - limits.retentionMillis) Long.MAX_VALUE
                else now + limits.retentionMillis
            Active(header, deadline).also {
                transfers[key] = it
                activeTransfers++
                bufferedBytes += header.totalBytes
            }
        }
        val offset = header.index * MeshFragmentProtocol.PAYLOAD_BYTES
        val size = frame.size - MeshFragmentProtocol.HEADER_BYTES
        if (active.received[header.index]) {
            val same = (0 until size).all { active.bytes[offset + it] == frame[MeshFragmentProtocol.HEADER_BYTES + it] }
            if (same) return ReassemblyResult.Duplicate
            close(key, active)
            return rejected(FragmentRejection.CONFLICTING_DUPLICATE)
        }
        frame.copyInto(active.bytes, offset, MeshFragmentProtocol.HEADER_BYTES)
        active.received[header.index] = true
        active.receivedCount++
        if (active.receivedCount != header.count) return ReassemblyResult.Pending
        close(key, active)
        if (MeshFragmentProtocol.checksum(active.bytes) != header.checksum) {
            return rejected(FragmentRejection.INTEGRITY_FAILED)
        }
        return ReassemblyResult.Complete(active.bytes)
    }

    /** Returns number expired; includes both incomplete transfers and terminal tombstones. */
    fun cleanup(): Int = cleanupAt(now())

    /** Release a disconnected session. Its token must never be reused, even after this call. */
    fun discardSession(session: MeshLinkSession) {
        cleanup()
        val iterator = transfers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.session == session) {
                release(entry.value)
                iterator.remove()
            }
        }
    }

    fun snapshot(): ReassemblySnapshot {
        cleanup()
        return ReassemblySnapshot(activeTransfers, transfers.size, bufferedBytes)
    }

    private fun close(key: Key, active: Active) {
        release(active)
        transfers[key] = Closed(active.expiresAt)
    }

    private fun release(transfer: Transfer) {
        if (transfer is Active) {
            activeTransfers--
            bufferedBytes -= transfer.bytes.size
        }
    }

    private fun cleanupAt(now: Long): Int {
        var expired = 0
        val iterator = transfers.values.iterator()
        while (iterator.hasNext()) {
            val transfer = iterator.next()
            if (transfer.expiresAt <= now) {
                release(transfer)
                iterator.remove()
                expired++
            }
        }
        return expired
    }

    private fun now(): Long {
        val value = clock.nowMillis()
        check(value >= lastTime) { "MeshClock must be monotonic" }
        lastTime = value
        return value
    }

    private fun rejected(reason: FragmentRejection) = ReassemblyResult.Rejected(reason)
}
