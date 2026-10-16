package com.nicobutter.beaconchat.mesh

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** One write fits the mandatory ATT MTU of 23 (20 bytes after ATT overhead). */
object DirectBleProtocol {
    const val MAX_PACKET_BYTES = 20
    const val MAX_CALLSIGN_BYTES = 8

    data class Packet(val callsign: String, val message: String)

    fun callsign(value: String): String {
        val name = value.ifBlank { "UNKNOWN" }
        require(name.toByteArray(Charsets.UTF_8).size <= MAX_CALLSIGN_BYTES && ':' !in name) {
            "Callsign BLE: máximo 8 bytes UTF-8 y sin ':'"
        }
        strictBytes(name)
        return name
    }

    fun encodeCallsign(name: String): ByteArray = strictBytes(callsign(name))

    fun decodeCallsign(bytes: ByteArray): String {
        require(bytes.size in 1..MAX_CALLSIGN_BYTES) { "Callsign BLE inválido" }
        val name = decodeUtf8(bytes)
        require(name.isNotBlank()) { "Callsign BLE vacío" }
        return callsign(name)
    }

    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()

    fun messageLimit(name: String): Int = MAX_PACKET_BYTES - callsign(name).toByteArray(Charsets.UTF_8).size - 1

    fun encode(name: String, message: String): ByteArray {
        require(message.isNotBlank()) { "El mensaje está vacío" }
        val packet = strictBytes("${callsign(name)}:$message")
        require(packet.size <= MAX_PACKET_BYTES) {
            "Mensaje demasiado largo: máximo ${messageLimit(name)} bytes UTF-8"
        }
        return packet
    }

    fun decode(bytes: ByteArray): Packet {
        require(bytes.size in 3..MAX_PACKET_BYTES) { "Tamaño de mensaje inválido" }
        val text = decodeUtf8(bytes)
        val separator = text.indexOf(':')
        require(separator > 0 && text.substring(0, separator).isNotBlank()) { "Mensaje sin callsign" }
        val packet = Packet(text.substring(0, separator), text.substring(separator + 1))
        encode(packet.callsign, packet.message)
        return packet
    }

    private fun strictBytes(text: String): ByteArray {
        val buffer = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(java.nio.CharBuffer.wrap(text))
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }
}
