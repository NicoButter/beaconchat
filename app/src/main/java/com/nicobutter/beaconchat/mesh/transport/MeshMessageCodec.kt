package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.core.MeshMessage
import com.nicobutter.beaconchat.mesh.core.MessageId
import com.nicobutter.beaconchat.mesh.core.NodeId
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.UUID

/**
 * V1, big endian: version:u8, flags:u8 (bit 0 = unicast), TTL:u8,
 * origin:UUID128, message:UUID128, [destination:UUID128], UTF-8 length:u16, UTF-8 bytes.
 * UUIDs are logical identities supplied by the caller, never MAC addresses or callsigns.
 * Invalid input throws IllegalArgumentException; no truncation, normalization or replacement.
 */
object MeshMessageCodec {
    const val MAX_CONTENT_BYTES = 1024
    const val MAX_HOPS = 16
    const val BROADCAST_HEADER_BYTES = 37
    const val UNICAST_HEADER_BYTES = 53
    const val MAX_ENCODED_BYTES = UNICAST_HEADER_BYTES + MAX_CONTENT_BYTES

    fun encode(message: MeshMessage): ByteArray {
        require(message.version == MeshMessage.VERSION) { "Unsupported mesh version" }
        require(message.hopsRemaining in 0..MAX_HOPS) { "Invalid TTL" }
        require(message.content.length in 1..MAX_CONTENT_BYTES && !message.content.isBlank()) {
            "Invalid content length"
        }
        val encoded = try {
            Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(message.content))
        } catch (e: CharacterCodingException) { throw IllegalArgumentException("Invalid UTF-16 input", e) }
        require(encoded.remaining() <= MAX_CONTENT_BYTES) { "Content exceeds UTF-8 byte limit" }
        val headerSize = if (message.destinationId == null) BROADCAST_HEADER_BYTES else UNICAST_HEADER_BYTES
        val out = ByteBuffer.allocate(headerSize + encoded.remaining())
        out.put(message.version.toByte()).put(if (message.destinationId == null) 0.toByte() else 1.toByte())
            .put(message.hopsRemaining.toByte())
        putUuid(out, message.originId.value)
        putUuid(out, message.messageId.value)
        message.destinationId?.let { putUuid(out, it.value) }
        out.putShort(encoded.remaining().toShort()).put(encoded)
        return out.array()
    }

    fun decode(bytes: ByteArray): MeshMessage {
        require(bytes.size in BROADCAST_HEADER_BYTES + 1..MAX_ENCODED_BYTES) { "Invalid envelope size" }
        val input = ByteBuffer.wrap(bytes)
        val version = input.get().toInt() and 0xff
        require(version == MeshMessage.VERSION) { "Unsupported mesh version" }
        val flags = input.get().toInt() and 0xff
        require(flags == 0 || flags == 1) { "Unknown mesh flags" }
        val ttl = input.get().toInt() and 0xff
        require(ttl in 0..MAX_HOPS) { "Invalid TTL" }
        val headerSize = if (flags == 0) BROADCAST_HEADER_BYTES else UNICAST_HEADER_BYTES
        require(bytes.size > headerSize) { "Truncated envelope" }
        val origin = NodeId(getUuid(input))
        val message = MessageId(getUuid(input))
        val destination = if (flags == 1) NodeId(getUuid(input)) else null
        val length = input.short.toInt() and 0xffff
        require(length in 1..MAX_CONTENT_BYTES && length == input.remaining()) { "Invalid content length" }
        val text = try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(input).toString()
        } catch (e: CharacterCodingException) { throw IllegalArgumentException("Invalid UTF-8 content", e) }
        require(!text.isBlank()) { "Blank mesh content" }
        return MeshMessage(message, origin, destination, text, ttl, version)
    }

    private fun putUuid(buffer: ByteBuffer, uuid: UUID) {
        buffer.putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits)
    }

    private fun getUuid(buffer: ByteBuffer) = UUID(buffer.long, buffer.long)
}
