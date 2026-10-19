package com.nicobutter.beaconchat.sos

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.UUID

/**
 * ASCII prefix + canonical unpadded base64url of a big-endian binary V1 record.
 * Keeps strict UTF-8 notes compact without JSON dependencies or changing MeshMessage's codec.
 * Encoding a local incident does NOT authorize its transmission. SosMeshPolicy is mandatory at I/O.
 */
object SosCodec {
    const val PREFIX = "BCSOS:"
    const val HEADER_BYTES = 30
    const val MAX_BINARY_BYTES = HEADER_BYTES + 34 + SosIncident.MAX_NOTE_BYTES
    const val MAX_CONTENT_BYTES = PREFIX.length + (MAX_BINARY_BYTES * 8 + 5) / 6 // 433, within mesh's 1024.

    fun encode(incident: SosIncident): String {
        val note = incident.note?.let(::strictUtf8) ?: byteArrayOf()
        val position = incident.position
        val flags = (if (position != null) 1 else 0) or (if (position?.accuracyMeters != null) 2 else 0) or
            (if (incident.laboratory) 4 else 0)
        val out = ByteBuffer.allocate(HEADER_BYTES + (if (position == null) 0 else if (position.accuracyMeters == null) 26 else 34) + note.size)
        out.put(incident.version.toByte()).put(flags.toByte())
            .putLong(incident.incidentId.mostSignificantBits).putLong(incident.incidentId.leastSignificantBits)
            .putLong(incident.createdAtEpochMillis).put(incident.type.wireId.toByte()).put(incident.severity.wireId.toByte())
        if (position != null) {
            out.put(position.source.wireId.toByte()).put(position.validity.wireId.toByte())
                .putDouble(position.latitude).putDouble(position.longitude).putLong(position.ageMillis)
            position.accuracyMeters?.let(out::putDouble)
        }
        out.putShort(note.size.toShort()).put(note)
        return PREFIX + SosBase64.encode(out.array())
    }

    fun decode(content: String): SosIncident {
        require(content.length in PREFIX.length + 1..MAX_CONTENT_BYTES && content.startsWith(PREFIX)) { "Contenido SOS inválido" }
        val encoded = content.substring(PREFIX.length)
        require(encoded.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }) { "Base64 SOS inválido" }
        val bytes = try { SosBase64.decode(encoded) } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Base64 SOS inválido", e)
        }
        require(bytes.size in HEADER_BYTES..MAX_BINARY_BYTES &&
            SosBase64.encode(bytes) == encoded) { "Longitud/codificación SOS no canónica" }
        val input = ByteBuffer.wrap(bytes)
        val version = input.get().toInt() and 0xff
        require(version == SosIncident.VERSION) { "Versión SOS desconocida" }
        val flags = input.get().toInt() and 0xff
        require(flags and 7 == flags && (flags and 2 == 0 || flags and 1 != 0)) { "Flags SOS inválidos" }
        val id = UUID(input.long, input.long)
        val created = input.long
        val typeId = input.get().toInt() and 0xff
        val severityId = input.get().toInt() and 0xff
        val type = EmergencyType.values().singleOrNull { it.wireId == typeId }
            ?: throw IllegalArgumentException("Tipo SOS desconocido")
        val severity = DeclaredSeverity.values().singleOrNull { it.wireId == severityId }
            ?: throw IllegalArgumentException("Gravedad SOS desconocida")
        val position = if (flags and 1 == 0) null else {
            require(input.remaining() >= (if (flags and 2 == 0) 26 else 34) + 2) { "Ubicación SOS truncada" }
            val sourceId = input.get().toInt() and 0xff
            val validityId = input.get().toInt() and 0xff
            val source = PositionSource.values().singleOrNull { it.wireId == sourceId }
                ?: throw IllegalArgumentException("Fuente SOS desconocida")
            val validity = PositionValidity.values().singleOrNull { it.wireId == validityId }
                ?: throw IllegalArgumentException("Validez SOS desconocida")
            val lat = input.double
            val lon = input.double
            val age = input.long
            val accuracy = if (flags and 2 == 0) null else input.double
            SosPosition(lat, lon, accuracy, source, age, validity)
        }
        val length = input.short.toInt() and 0xffff
        require(length <= SosIncident.MAX_NOTE_BYTES && input.remaining() == length) { "Longitud de nota SOS inválida" }
        val note = if (length == 0) null else try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(input).toString()
        } catch (e: CharacterCodingException) { throw IllegalArgumentException("Nota SOS UTF-8 inválida", e) }
        return SosIncident(id, type, severity, created, note, position, flags and 4 != 0, version)
    }
}

/** Small bounded base64url implementation also usable on Android 24–25 (java.util.Base64 is API 26). */
private object SosBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    fun encode(bytes: ByteArray): String {
        val out = StringBuilder()
        var accumulator = 0
        var bits = 0
        for (byte in bytes) {
            accumulator = (accumulator shl 8) or (byte.toInt() and 255)
            bits += 8
            while (bits >= 6) { bits -= 6; out.append(ALPHABET[(accumulator ushr bits) and 63]) }
        }
        if (bits > 0) out.append(ALPHABET[(accumulator shl (6 - bits)) and 63])
        return out.toString()
    }
    fun decode(value: String): ByteArray {
        require(value.length <= SosCodec.MAX_CONTENT_BYTES - SosCodec.PREFIX.length && value.length % 4 != 1)
        val out = ByteArray(value.length * 6 / 8)
        var accumulator = 0
        var bits = 0
        var index = 0
        for (char in value) {
            val digit = ALPHABET.indexOf(char)
            require(digit >= 0)
            accumulator = (accumulator shl 6) or digit
            bits += 6
            if (bits >= 8) { bits -= 8; out[index++] = (accumulator ushr bits).toByte() }
        }
        return out
    }
}
