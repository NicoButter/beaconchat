package com.nicobutter.beaconchat.sos

import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.UUID

enum class EmergencyType(val wireId: Int) { MEDICAL(1), FIRE(2), TRAPPED(3), OTHER(4) }
enum class DeclaredSeverity(val wireId: Int) { LOW(1), MODERATE(2), HIGH(3), CRITICAL(4) }
enum class PositionSource(val wireId: Int) { GNSS(1), SYNTHETIC(2), MOCK_GPS(3) }
enum class PositionValidity(val wireId: Int) { CURRENT(1), LAST_KNOWN(2), STALE(3) }

/** Age is a snapshot at SOS creation, not a synchronized clock or a guarantee of accuracy. */
data class SosPosition(val latitude: Double, val longitude: Double, val accuracyMeters: Double?,
    val source: PositionSource, val ageMillis: Long, val validity: PositionValidity) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitud inválida" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitud inválida" }
        require(accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0)) { "Precisión inválida" }
        require(ageMillis >= 0) { "Antigüedad inválida" }
        require(when (validity) {
            PositionValidity.CURRENT -> ageMillis <= FRESH_MILLIS
            PositionValidity.LAST_KNOWN -> ageMillis <= RECENT_MILLIS
            PositionValidity.STALE -> ageMillis > RECENT_MILLIS
        }) { "La antigüedad no coincide con la validez declarada" }
    }
    companion object {
        const val FRESH_MILLIS = 30_000L
        const val RECENT_MILLIS = 300_000L
    }
}

/** Immutable incident: retransmission/retry preserves this UUID, creation time and payload. */
data class SosIncident(val incidentId: UUID, val type: EmergencyType, val severity: DeclaredSeverity,
    val createdAtEpochMillis: Long, val note: String? = null, val position: SosPosition? = null,
    val laboratory: Boolean = false, val version: Int = VERSION) {
    init {
        require(version == VERSION) { "Versión SOS desconocida" }
        require(incidentId != UUID(0, 0)) { "UUID de incidente nulo" }
        require(createdAtEpochMillis >= 0) { "Creación inválida" }
        note?.let {
            require(it.length in 1..MAX_NOTE_BYTES && !it.isBlank()) { "Nota vacía o demasiado larga" }
            require(strictUtf8(it).size <= MAX_NOTE_BYTES) { "Nota: máximo 256 bytes UTF-8" }
        }
    }
    companion object {
        const val VERSION = 1
        const val MAX_NOTE_BYTES = 256
    }
}

internal fun strictUtf8(text: String): ByteArray = try {
    val buffer = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(text))
    ByteArray(buffer.remaining()).also { buffer.get(it) }
} catch (e: CharacterCodingException) { throw IllegalArgumentException("UTF-16 inválido", e) }
