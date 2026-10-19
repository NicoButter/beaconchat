package com.nicobutter.beaconchat.mesh.transport

import java.nio.ByteBuffer
import java.util.UUID
import java.util.zip.CRC32

/** Reserved for a FUTURE dedicated GATT characteristic. Not registered or used by the BLE chat. */
object MeshGattContract {
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("7d2ea28a-f7bd-4f56-9e75-6e57c1e9b501")
}

/**
 * V1, big endian: marker:u8, version:u8, transfer:u16, index:u8, count:u8,
 * total bytes:u16, whole-payload CRC32:u32, followed by 1..8 bytes.
 * Every non-final frame carries exactly 8 bytes. Index is zero based; count/length must agree.
 * CRC32 detects accidental corruption/mixing; it is NOT authentication or encryption.
 */
object MeshFragmentProtocol {
    const val VERSION = 1
    const val MARKER = 0xf9 // Cannot start a valid UTF-8 legacy packet; no text-based auto-detection.
    const val MAX_FRAME_BYTES = 20
    const val HEADER_BYTES = 12
    const val PAYLOAD_BYTES = MAX_FRAME_BYTES - HEADER_BYTES
    const val MAX_TRANSFER_BYTES = MeshMessageCodec.MAX_ENCODED_BYTES
    const val MAX_FRAGMENTS = (MAX_TRANSFER_BYTES + PAYLOAD_BYTES - 1) / PAYLOAD_BYTES

    /** Caller allocates an ID per link session and must not reuse it during receiver retention. */
    fun fragment(payload: ByteArray, transferId: Int): List<ByteArray> {
        require(payload.size in 1..MAX_TRANSFER_BYTES) { "Invalid transfer size" }
        require(transferId in 0..0xffff) { "Invalid transfer ID" }
        val count = countFor(payload.size)
        val checksum = checksum(payload)
        return List(count) { index ->
            val offset = index * PAYLOAD_BYTES
            val size = minOf(PAYLOAD_BYTES, payload.size - offset)
            ByteBuffer.allocate(HEADER_BYTES + size).put(MARKER.toByte()).put(VERSION.toByte())
                .putShort(transferId.toShort()).put(index.toByte()).put(count.toByte())
                .putShort(payload.size.toShort()).putInt(checksum.toInt())
                .put(payload, offset, size).array()
        }
    }

    internal data class Header(val transferId: Int, val index: Int, val count: Int,
        val totalBytes: Int, val checksum: Long)

    /** Validates before any transfer allocation; a frame never contains an unbounded length. */
    internal fun parse(frame: ByteArray): Header {
        require(frame.size in HEADER_BYTES + 1..MAX_FRAME_BYTES) { "Invalid frame size" }
        val input = ByteBuffer.wrap(frame)
        require((input.get().toInt() and 0xff) == MARKER) { "Not a mesh frame" }
        require((input.get().toInt() and 0xff) == VERSION) { "Unsupported fragment version" }
        val transfer = input.short.toInt() and 0xffff
        val index = input.get().toInt() and 0xff
        val count = input.get().toInt() and 0xff
        val total = input.short.toInt() and 0xffff
        val checksum = input.int.toLong() and 0xffffffffL
        require(total in 1..MAX_TRANSFER_BYTES && count in 1..MAX_FRAGMENTS) { "Transfer too large" }
        require(count == countFor(total) && index < count) { "Inconsistent fragment position/count" }
        val expected = if (index == count - 1) total - index * PAYLOAD_BYTES else PAYLOAD_BYTES
        require(input.remaining() == expected) { "Invalid fragment payload length" }
        return Header(transfer, index, count, total, checksum)
    }

    internal fun checksum(bytes: ByteArray): Long = CRC32().also { it.update(bytes) }.value
    private fun countFor(size: Int) = (size + PAYLOAD_BYTES - 1) / PAYLOAD_BYTES
}
