package com.nicobutter.beaconchat.mesh.core

import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.util.ArrayDeque
import java.util.UUID

/** All limits are per node. Full caches/queues reject the newest item, never evict a live one. */
data class MeshLimits(
    val maxSeenMessages: Int = 256,
    val maxPendingMessages: Int = 64,
    val seenRetentionMillis: Long = 120_000,
    val pendingRetentionMillis: Long = 30_000,
    val maxHops: Int = 16,
    val maxContentBytes: Int = 1024
) {
    init {
        require(maxSeenMessages > 0 && maxPendingMessages > 0)
        require(seenRetentionMillis > 0 && pendingRetentionMillis in 1..seenRetentionMillis)
        require(maxHops > 0 && maxContentBytes > 0)
    }
}

enum class Acceptance { ACCEPTED, DUPLICATE, OWN_ORIGIN, UNSUPPORTED_VERSION, INVALID_MESSAGE, SEEN_CAPACITY }
enum class Forwarding { NOT_APPLICABLE, LOCAL_DESTINATION, TTL_EXHAUSTED, QUEUED, QUEUE_FULL }

data class MeshDecision(
    val message: MeshMessage,
    val acceptance: Acceptance,
    /** Consume this once as a local delivery event; it says nothing about another node's delivery. */
    val deliverLocally: Boolean = false,
    val forwarding: Forwarding = Forwarding.NOT_APPLICABLE
)

enum class DispatchStatus { HANDED_OFF, UNAVAILABLE, TRANSPORT_ERROR }
data class DispatchResult(val message: MeshMessage, val status: DispatchStatus)
data class MeshSnapshot(val seenMessages: Int, val pendingMessages: Int)
data class CleanupResult(val seenExpired: Int, val pendingExpired: Int)

/**
 * Transport-free best-effort flooding, serialized by the caller (not thread safe).
 * No Android, timers, background work, routing discovery or automatic retry.
 * Idempotence holds while a key is retained. TTL bounds each copy even after cache expiry.
 */
class MeshEngine(
    val nodeId: NodeId,
    private val clock: MeshClock,
    private val limits: MeshLimits = MeshLimits(),
    private val nextMessageId: () -> MessageId = { MessageId(UUID.randomUUID()) }
) {
    private data class Pending(val message: MeshMessage, val excluded: Set<NodeId>, val expiresAt: Long)
    private val seen = LinkedHashMap<MessageKey, Long>()
    private val pending = ArrayDeque<Pending>()
    private var lastTime = Long.MIN_VALUE

    /** Retries can explicitly reuse messageId. Locally created messages always use this node's ID. */
    fun originate(
        destinationId: NodeId?,
        content: String,
        hopsRemaining: Int = limits.maxHops,
        messageId: MessageId = nextMessageId()
    ): MeshDecision = accept(MeshMessage(messageId, nodeId, destinationId, content, hopsRemaining), null)

    fun receive(message: MeshMessage, previousHop: NodeId): MeshDecision {
        if (message.originId == nodeId) {
            cleanup()
            return MeshDecision(message, Acceptance.OWN_ORIGIN)
        }
        return accept(message, previousHop)
    }

    private fun accept(message: MeshMessage, previousHop: NodeId?): MeshDecision {
        val now = now()
        cleanupAt(now)
        if (message.version != MeshMessage.VERSION) return MeshDecision(message, Acceptance.UNSUPPORTED_VERSION)
        if (!valid(message)) return MeshDecision(message, Acceptance.INVALID_MESSAGE)
        if (seen.containsKey(message.key)) return MeshDecision(message, Acceptance.DUPLICATE)
        if (seen.size >= limits.maxSeenMessages) return MeshDecision(message, Acceptance.SEEN_CAPACITY)
        // Record before exposing local delivery or handing anything to the transport.
        seen[message.key] = deadline(now, limits.seenRetentionMillis)
        val local = message.destinationId == null || message.destinationId == nodeId
        val forwarding = when {
            message.destinationId == nodeId -> Forwarding.LOCAL_DESTINATION
            message.hopsRemaining == 0 -> Forwarding.TTL_EXHAUSTED
            pending.size >= limits.maxPendingMessages -> Forwarding.QUEUE_FULL
            else -> {
                val excluded = if (previousHop == null) setOf(nodeId) else setOf(nodeId, previousHop)
                pending.addLast(Pending(message.copy(hopsRemaining = message.hopsRemaining - 1),
                    excluded, deadline(now, limits.pendingRetentionMillis)))
                Forwarding.QUEUED
            }
        }
        return MeshDecision(message, Acceptance.ACCEPTED, local, forwarding)
    }

    /**
     * FIFO, at most maxMessages and at most the initial queue size. Each item is attempted once.
     * Failure is explicit and terminal for that queued copy, with no fabricated delivery ACK.
     * A transport must not block; an asynchronous adapter must do its own bounded handoff.
     */
    fun dispatch(transport: MeshTransport, maxMessages: Int = limits.maxPendingMessages): List<DispatchResult> {
        require(maxMessages > 0)
        cleanup()
        val budget = minOf(maxMessages, pending.size)
        val results = mutableListOf<DispatchResult>()
        repeat(budget) {
            cleanup()
            val item = pending.pollFirst() ?: return@repeat
            val status = try {
                when (transport.forward(item.message, item.excluded)) {
                    Handoff.ACCEPTED -> DispatchStatus.HANDED_OFF
                    Handoff.UNAVAILABLE -> DispatchStatus.UNAVAILABLE
                }
            } catch (_: Exception) { DispatchStatus.TRANSPORT_ERROR }
            results.add(DispatchResult(item.message, status))
        }
        return results
    }

    /** Called on every public operation; hosts may also call this while idle. */
    fun cleanup(): CleanupResult = cleanupAt(now())

    fun snapshot(): MeshSnapshot {
        cleanup()
        return MeshSnapshot(seen.size, pending.size)
    }

    private fun cleanupAt(now: Long): CleanupResult {
        val oldSeen = seen.size
        val oldPending = pending.size
        seen.entries.removeAll { it.value <= now }
        while (pending.peekFirst()?.expiresAt?.let { it <= now } == true) pending.removeFirst()
        return CleanupResult(oldSeen - seen.size, oldPending - pending.size)
    }

    private fun valid(message: MeshMessage): Boolean {
        if (message.hopsRemaining !in 0..limits.maxHops || message.content.isBlank() ||
            message.content.length > limits.maxContentBytes) return false
        return try {
            Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(message.content)).remaining() <= limits.maxContentBytes
        } catch (_: java.nio.charset.CharacterCodingException) { false }
    }

    private fun now(): Long {
        val value = clock.nowMillis()
        check(value >= lastTime) { "MeshClock must be monotonic" }
        lastTime = value
        return value
    }

    private fun deadline(now: Long, duration: Long): Long =
        if (now > Long.MAX_VALUE - duration) Long.MAX_VALUE else now + duration
}
