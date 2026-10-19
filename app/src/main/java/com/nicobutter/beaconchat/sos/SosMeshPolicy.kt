package com.nicobutter.beaconchat.sos

import java.util.UUID

/** Local privacy boundary, NOT authentication: remote declarations are still untrusted. */
object SosMeshPolicy {
    val SYNTHETIC_POSITION = SosPosition(12.3456, -45.6789, 25.0, PositionSource.SYNTHETIC, 0, PositionValidity.CURRENT)

    /** Never copies a real coordinate or a local note into an over-the-air laboratory incident. */
    fun laboratoryIncident(withPosition: Boolean, type: EmergencyType, severity: DeclaredSeverity,
        createdAtEpochMillis: Long, incidentId: UUID = UUID.randomUUID()) = SosIncident(incidentId, type, severity,
        createdAtEpochMillis, "LAB: ensayo sintético, no emergencia real", if (withPosition) SYNTHETIC_POSITION else null, laboratory = true)

    fun permitted(incident: SosIncident): Boolean = incident.laboratory &&
        (incident.position == null || incident.position == SYNTHETIC_POSITION)

    fun contentPermitted(content: String): Boolean {
        if (!content.startsWith(SosCodec.PREFIX)) return true // Normal chat retains its existing rules.
        return try { permitted(SosCodec.decode(content)) } catch (_: IllegalArgumentException) { false }
    }
}

enum class SosSubmissionStatus { ORIGINATED_LOCALLY, REJECTED, PRIVACY_BLOCKED }
data class SosSubmission(val incidentId: UUID, val status: SosSubmissionStatus)

/** A retry reuses the immutable SOS UUID/content, while MeshEngine allocates a new attempt messageId. */
class SosMeshGateway(private val originate: (String) -> Boolean) {
    fun submit(incident: SosIncident): SosSubmission {
        if (!SosMeshPolicy.permitted(incident)) return SosSubmission(incident.incidentId, SosSubmissionStatus.PRIVACY_BLOCKED)
        return SosSubmission(incident.incidentId, if (originate(SosCodec.encode(incident)))
            SosSubmissionStatus.ORIGINATED_LOCALLY else SosSubmissionStatus.REJECTED)
    }
}
