package com.nicobutter.beaconchat.sos

/** The timestamp is from the same device's monotonic clock, never another node's wall clock. */
data class GpsSample(val latitude: Double, val longitude: Double, val accuracyMeters: Double?,
    val elapsedRealtimeMillis: Long, val mock: Boolean = false) {
    init {
        require(elapsedRealtimeMillis >= 0)
        SosPosition(latitude, longitude, accuracyMeters, if (mock) PositionSource.MOCK_GPS else PositionSource.GNSS,
            0, PositionValidity.LAST_KNOWN)
    }
    fun position(now: Long, allowCurrent: Boolean): SosPosition {
        require(now >= elapsedRealtimeMillis) { "Fix con tiempo monotónico futuro" }
        val age = now - elapsedRealtimeMillis
        val validity = when {
            allowCurrent && age <= SosPosition.FRESH_MILLIS -> PositionValidity.CURRENT
            age <= SosPosition.RECENT_MILLIS -> PositionValidity.LAST_KNOWN
            else -> PositionValidity.STALE
        }
        return SosPosition(latitude, longitude, accuracyMeters,
            if (mock) PositionSource.MOCK_GPS else PositionSource.GNSS, age, validity)
    }
}

enum class GnssStatus { IDLE, SEARCHING, CURRENT, LAST_KNOWN, NO_POSITION, PERMISSION_REQUIRED, CANCELLED, ERROR }
data class GnssSnapshot(val status: GnssStatus, val position: SosPosition?, val detail: String)

/** Single bounded request, shared by Android and JVM tests. Late/cancelled callback tokens are ignored. */
class GnssAcquisition {
    private var serial = 0L
    private var active: Long? = null
    private var deadline = 0L
    private var sample: GpsSample? = null
    private var status = GnssStatus.IDLE
    private var detail = "Ubicación no solicitada"
    val searching get() = active != null

    fun start(now: Long, permitted: Boolean, providerEnabled: Boolean, cached: GpsSample?, timeoutMillis: Long): Long? {
        require(now >= 0 && timeoutMillis in 1..120_000) { "Timeout GNSS: 1..120000 ms" }
        serial++
        active = null
        sample = null
        if (!permitted) { status = GnssStatus.PERMISSION_REQUIRED; detail = "Se requiere ubicación precisa; solo aproximada no habilita GPS"; return null }
        sample = cached?.takeIf { it.elapsedRealtimeMillis <= now }
        if (!providerEnabled) {
            status = if (sample == null) GnssStatus.NO_POSITION else GnssStatus.LAST_KNOWN
            detail = "GPS no disponible/apagado; no se obtuvo un fix actual"
            return null
        }
        deadline = if (now > Long.MAX_VALUE - timeoutMillis) Long.MAX_VALUE else now + timeoutMillis
        status = GnssStatus.SEARCHING
        detail = "Buscando GPS local; la alternativa conocida no es un fix actual"
        active = serial
        return serial
    }

    fun location(token: Long, value: GpsSample, now: Long): Boolean {
        if (active != token || value.elapsedRealtimeMillis > now) return false
        if (now >= deadline) { timeout(token, now); return false }
        if (sample == null || value.elapsedRealtimeMillis > sample!!.elapsedRealtimeMillis) sample = value
        if (now - value.elapsedRealtimeMillis > SosPosition.FRESH_MILLIS) return false
        sample = value
        active = null
        status = GnssStatus.CURRENT
        detail = if (value.mock) "Posición marcada mock por Android; no es evidencia satelital" else "Fix GPS reciente; adquisición satelital no certificada"
        return true
    }

    fun timeout(token: Long, now: Long): Boolean {
        if (active != token || now < deadline) return false
        active = null
        status = if (sample == null) GnssStatus.NO_POSITION else GnssStatus.LAST_KNOWN
        detail = "Timeout GPS; sin fix actual"
        return true
    }

    fun unavailable(now: Long, reason: String) {
        active = null
        status = if (sample == null) GnssStatus.NO_POSITION else GnssStatus.LAST_KNOWN
        detail = reason
        snapshot(now)
    }

    fun error(reason: String) { active = null; sample = null; status = GnssStatus.ERROR; detail = reason }
    fun cancel() { active = null; sample = null; status = GnssStatus.CANCELLED; detail = "Captura cancelada; posición descartada" }

    fun snapshot(now: Long): GnssSnapshot {
        val position = sample?.position(now, status == GnssStatus.CURRENT)
        val shownStatus = if (status == GnssStatus.CURRENT && position?.validity != PositionValidity.CURRENT) GnssStatus.LAST_KNOWN else status
        return GnssSnapshot(shownStatus, position, if (shownStatus != status) "Fix previo envejecido; no es ubicación actual" else detail)
    }
}
