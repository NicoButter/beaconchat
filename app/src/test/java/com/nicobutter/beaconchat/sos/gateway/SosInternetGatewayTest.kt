package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.sos.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.net.ssl.SSLHandshakeException

internal val labEndpoint get() = LabEndpoint.parse("https://api.example.invalid${LabEndpoint.PATH}")
internal fun response(id: UUID, status: Int = 201, receipt: String = "lab_fake_1") = LabHttpResponse(status,
    "application/json; charset=utf-8",
    "{\"version\":\"1\",\"incidentId\":\"$id\",\"receipt\":\"$receipt\",\"persistedInLaboratory\":true}")

/** Simulates idempotent lab persistence, never contacts a network or an emergency service. */
internal class FakeLabServer : LabHttpTransport {
    val requests = mutableListOf<LabHttpRequest>()
    val saved = linkedMapOf<UUID, String>()
    var behavior: ((LabHttpRequest) -> LabHttpResponse)? = null
    override fun post(request: LabHttpRequest): LabHttpResponse {
        requests.add(request)
        behavior?.let { return it(request) }
        val id = request.payload.incidentId
        val old = saved[id]
        if (old != null && old != request.payload.json()) return LabHttpResponse(409, null, "")
        saved[id] = request.payload.json()
        return response(id)
    }
}

class SosInternetGatewayTest {
    private val clock = FakeClock()
    private fun gateway(limits: GatewayLimits = GatewayLimits()) = SosInternetGateway(clock, limits).also { it.enable(labEndpoint) }
    private fun state(gateway: SosInternetGateway) = gateway.snapshot().incidents.single()
    private fun newIncident() = incident().copy(incidentId = UUID.randomUUID())

    @Test fun defaultIsDisabledAndNeverCreatesAnIncidentOrHttpAttempt() {
        val g = SosInternetGateway(clock)
        assertEquals(GatewayAdmission.DISABLED, g.receive(incident()))
        assertNull(g.beginDelivery(true)); assertEquals(0, g.snapshot().tracked)
    }
    @Test fun receivedQueuedStartedAndCorrelatedLabPersistenceAreDistinctEvents() {
        val g = gateway(); val sos = incident(); val server = FakeLabServer()
        assertEquals(GatewayAdmission.QUEUED, g.receive(sos))
        assertEquals(listOf(GatewayState.RECEIVED, GatewayState.QUEUED), g.snapshot().events.map { it.state })
        assertNull(state(g).receipt)
        val attempt = g.beginDelivery(true)!!
        assertEquals(GatewayState.HTTP_STARTED, state(g).state)
        assertNull(state(g).receipt)
        assertTrue(g.complete(attempt, LabApiClient(server).deliver(attempt.request)))
        assertEquals(GatewayState.LAB_CONFIRMED, state(g).state)
        assertEquals("lab_fake_1", state(g).receipt)
        assertEquals(sos.incidentId, server.saved.keys.single())
        assertEquals(0, g.snapshot().queued)
    }
    @Test fun equivalentCopiesBeforeAndAfterConfirmationNeverCreateNewDelivery() {
        val g = gateway(); val sos = incident(); g.receive(sos)
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(SosCodec.decode(SosCodec.encode(sos))))
        val a = g.beginDelivery(true)!!
        g.complete(a, LabDeliveryResult.Confirmed(LabReceipt(sos.incidentId, "lab_one")))
        clock.time += 5000
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(sos))
        assertNull(g.beginDelivery(true)); assertEquals(1, state(g).attempts)
    }
    @Test fun sameUuidWithChangedContentIsAConflictEvenWhenOnlyTheOmittedNoteDiffers() {
        val g = gateway(); val original = incident(note = "nota A"); g.receive(original)
        for (other in listOf(original.copy(note = "nota B"), original.copy(createdAtEpochMillis = 2),
            original.copy(type = EmergencyType.FIRE), original.copy(severity = DeclaredSeverity.LOW),
            original.copy(position = SosMeshPolicy.SYNTHETIC_POSITION))) {
            assertEquals(GatewayAdmission.CONFLICT, g.receive(other))
        }
        assertEquals(1, g.snapshot().tracked)
        assertEquals(LabIncidentPayload.from(original), g.beginDelivery(true)!!.request.payload)
    }
    @Test fun queueSaturationRejectsNewestWithoutEvictingPendingAndRetainsRejectionTombstone() {
        val g = gateway(GatewayLimits(maxQueued = 1, maxTracked = 3))
        val one = newIncident(); val two = newIncident()
        g.receive(one)
        assertEquals(GatewayAdmission.QUEUE_FULL, g.receive(two))
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(two))
        assertEquals(1, g.snapshot().queued)
        assertEquals(one.incidentId, g.beginDelivery(true)!!.request.payload.incidentId)
    }
    @Test fun retainedFingerprintCapacityRejectsNewUuidInsteadOfEvictingLiveDedupe() {
        val g = gateway(GatewayLimits(maxQueued = 1, maxTracked = 1))
        g.receive(incident()); val a = g.beginDelivery(true)!!
        g.complete(a, LabDeliveryResult.Rejected(422))
        assertEquals(GatewayAdmission.MEMORY_FULL, g.receive(newIncident()))
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(incident()))
    }
    @Test fun offlineQueueDoesNotStartHttpAndReconnectCanDeliver() {
        val g = gateway(); g.receive(incident())
        repeat(3) { clock.time += 1000; assertNull(g.beginDelivery(false)) }
        assertEquals(0, state(g).attempts)
        assertFalse(g.snapshot().connected)
        assertNotNull(g.beginDelivery(true))
    }
    @Test fun queueExpiresUsingLocalMonotonicTimeIndependentOfSenderClock() {
        val g = gateway(GatewayLimits(queueLifetimeMillis = 10, dedupLifetimeMillis = 100))
        val future = incident().copy(createdAtEpochMillis = Long.MAX_VALUE)
        g.receive(future); clock.time = 10
        assertNull(g.beginDelivery(true)); assertEquals(GatewayState.EXPIRED, state(g).state)
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(future))
        clock.time = 100
        assertEquals(0, g.snapshot().tracked)
        assertEquals(GatewayAdmission.QUEUED, g.receive(future)) // Deliberate, documented volatile retention boundary.
    }
    @Test fun globalSendRateAndSingleFlightApplyAcrossDifferentIncidents() {
        val g = gateway(); g.receive(incident()); g.receive(newIncident())
        val a = g.beginDelivery(true)!!
        assertNull(g.beginDelivery(true))
        g.complete(a, LabDeliveryResult.Rejected(422))
        clock.time = 4999; assertNull(g.beginDelivery(true))
        clock.time = 5000; assertNotNull(g.beginDelivery(true))
    }
    @Test fun lostResponseAfterSaveRetriesTheSameUuidAndBytesWithBoundedBackoff() {
        val g = gateway(); val server = FakeLabServer(); g.receive(incident())
        server.behavior = { request -> server.saved[request.payload.incidentId] = request.payload.json(); throw IOException("lost") }
        val a = g.beginDelivery(true)!!
        g.complete(a, LabApiClient(server).deliver(a.request))
        assertEquals(GatewayState.UNKNOWN, state(g).state)
        assertNull(state(g).receipt)
        clock.time = 4999; assertNull(g.beginDelivery(true))
        clock.time = 5000; server.behavior = null
        val b = g.beginDelivery(true)!!
        g.complete(b, LabApiClient(server).deliver(b.request))
        assertNotEquals(a.token, b.token)
        assertEquals(a.request.idempotencyKey, b.request.idempotencyKey)
        assertEquals(a.request.payload.json(), b.request.payload.json())
        assertEquals(1, server.saved.size)
        assertEquals(GatewayState.LAB_CONFIRMED, state(g).state)
    }
    @Test fun retriesStopAfterThreeAttemptsAndKeepTheIncidentForDedupe() {
        val g = gateway(); g.receive(incident())
        val first = g.beginDelivery(true)!!; g.complete(first, LabDeliveryResult.Timeout)
        clock.time = 5000
        val second = g.beginDelivery(true)!!; g.complete(second, LabDeliveryResult.Unknown)
        clock.time = 14999; assertNull(g.beginDelivery(true))
        clock.time = 15000
        val third = g.beginDelivery(true)!!; g.complete(third, LabDeliveryResult.Error)
        clock.time = 100_000
        assertNull(g.beginDelivery(true)); assertEquals(3, state(g).attempts); assertFalse(state(g).pending)
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(incident()))
    }
    @Test fun watchdogTimesOutAndIgnoresLateConfirmation() {
        val g = gateway(); g.receive(incident()); val a = g.beginDelivery(true)!!
        clock.time = 20_000
        assertEquals(GatewayState.TIMEOUT, state(g).state)
        assertFalse(g.complete(a, LabDeliveryResult.Confirmed(LabReceipt(incident().incidentId, "lab_late"))))
        assertNull(state(g).receipt)
    }
    @Test fun expiryAfterStartingHttpKeepsPersistenceUnknown() {
        val g = gateway(GatewayLimits(queueLifetimeMillis = 10, dedupLifetimeMillis = 100))
        g.receive(incident()); val a = g.beginDelivery(true)!!
        clock.time = 10
        assertEquals(GatewayState.UNKNOWN, state(g).state)
        assertFalse(state(g).pending)
        assertFalse(g.complete(a, LabDeliveryResult.Confirmed(LabReceipt(incident().incidentId, "lab_late"))))
    }
    @Test fun disablingDiscardsQueuedAndMakesInFlightUnknownAndLateResultsHarmless() {
        val g = gateway(); g.receive(incident()); val waiting = newIncident(); g.receive(waiting)
        val a = g.beginDelivery(true)!!; g.disable()
        val statuses = g.snapshot().incidents.associate { it.incidentId to it.state }
        assertEquals(GatewayState.UNKNOWN, statuses[incident().incidentId])
        assertEquals(GatewayState.CANCELLED, statuses[waiting.incidentId])
        assertFalse(g.complete(a, LabDeliveryResult.Confirmed(LabReceipt(incident().incidentId, "lab_late"))))
        g.enable(labEndpoint); assertNull(g.beginDelivery(true))
        assertEquals(GatewayAdmission.DUPLICATE, g.receive(incident()))
    }
    @Test fun realGnssMockArbitrarySyntheticAndNonLabAreBlockedAtGatewayAndPayloadBoundary() {
        val g = gateway(); val p = SosMeshPolicy.SYNTHETIC_POSITION
        for (private in listOf(incident(p.copy(source = PositionSource.GNSS)),
            incident(p.copy(source = PositionSource.MOCK_GPS)), incident(p.copy(latitude = -51.6)),
            incident(laboratory = false))) {
            assertEquals(GatewayAdmission.PRIVACY_BLOCKED, g.receive(private))
            rejectsSos { LabIncidentPayload.from(private) }
        }
        assertEquals(0, g.snapshot().tracked); assertTrue(g.snapshot().events.isEmpty())
    }
    @Test fun historyAndMemoryAreBoundedAndExpiredEntriesReleaseCapacity() {
        val g = gateway(GatewayLimits(maxQueued = 1, maxTracked = 2, maxEvents = 3,
            queueLifetimeMillis = 10, dedupLifetimeMillis = 20))
        repeat(50) { g.receive(newIncident()) }
        assertEquals(2, g.snapshot().tracked); assertEquals(3, g.snapshot().events.size)
        clock.time = 20; assertEquals(0, g.snapshot().tracked)
        assertEquals(GatewayAdmission.QUEUED, g.receive(newIncident()))
    }
    @Test fun injectedClientCannotConfirmAnotherUuid() {
        val g = gateway(); g.receive(incident()); val a = g.beginDelivery(true)!!
        g.complete(a, LabDeliveryResult.Confirmed(LabReceipt(UUID.randomUUID(), "lab_wrong")))
        assertEquals(GatewayState.UNKNOWN, state(g).state); assertNull(state(g).receipt)
    }
    @Test fun apiTimeoutTlsFailureAndHttpRejectionRemainDistinct() {
        val request = LabHttpRequest(labEndpoint, LabIncidentPayload.from(incident()))
        assertEquals(LabDeliveryResult.Timeout, LabApiClient(LabHttpTransport { throw SocketTimeoutException() }).deliver(request))
        assertEquals(LabDeliveryResult.Error, LabApiClient(LabHttpTransport { throw SSLHandshakeException("untrusted") }).deliver(request))
        for (status in listOf(400, 401, 403, 409, 422)) {
            assertEquals(LabDeliveryResult.Rejected(status), LabApiClient(LabHttpTransport { LabHttpResponse(status, null, "") }).deliver(request))
        }
        val g = gateway(); g.receive(incident()); val a = g.beginDelivery(true)!!
        g.complete(a, LabDeliveryResult.Rejected(409)); clock.time = 5000
        assertNull(g.beginDelivery(true)); assertEquals(GatewayState.REJECTED, state(g).state)
    }
}
