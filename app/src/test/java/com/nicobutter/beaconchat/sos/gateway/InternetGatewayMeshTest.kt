package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import com.nicobutter.beaconchat.mesh.ble.*
import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import com.nicobutter.beaconchat.sos.*
import org.junit.Assert.*
import org.junit.Test

class InternetGatewayMeshTest {
    private fun chain(gateway: SosInternetGateway, observations: MutableList<String> = mutableListOf()): FakeGattNetwork {
        val net = FakeGattNetwork { name, message ->
            if (name == "C") { observations.add(message.content); gateway.onAcceptedMesh(message) }
        }
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C"); net.discover()
        return net
    }
    private fun confirm(g: SosInternetGateway, server: FakeLabServer) {
        val a = g.beginDelivery(true)!!
        assertTrue(g.complete(a, LabApiClient(server).deliver(a.request)))
        assertEquals(GatewayState.LAB_CONFIRMED, g.snapshot().incidents.single().state)
    }

    @Test fun abcGatewayReceivesTransitUnicastEvenWhenCIsNotTheLogicalDestination() {
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }
        val net = chain(g); val sos = incident(SosMeshPolicy.SYNTHETIC_POSITION)
        assertTrue(net.nodes.getValue("A").originate(node("D"), SosCodec.encode(sos), 2))
        assertEquals(0, g.snapshot().queued); net.run()
        assertTrue(net.received("C").isEmpty()) // No LOCAL_RECEIVED; explicit acceptance hook is required.
        assertEquals(1, g.snapshot().queued)
        assertFalse("A" to "C" in net.established)
        val server = FakeLabServer(); confirm(g, server)
        assertEquals(sos.incidentId, server.saved.keys.single())
        assertEquals("lab_fake_1", g.snapshot().incidents.single().receipt)
        assertTrue(net.nodes.getValue("A").snapshot().events.none { it.detail.contains("lab_fake_1") })
    }
    @Test fun destinationGatewayAlsoReceivesAcceptedSos() {
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }
        val net = chain(g)
        net.nodes.getValue("A").originate(node("C"), SosCodec.encode(incident()), 2); net.run()
        assertEquals(1, g.snapshot().queued)
        assertEquals(1, net.received("C").size)
        confirm(g, FakeLabServer())
    }
    @Test fun diamondWithDuplicateFramesAndTwoRoutesTriggersOnlyOneGatewayObservation() {
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }
        var observations = 0
        val net = FakeGattNetwork { at, message -> if (at == "C") { observations++; g.onAcceptedMesh(message) } }
        net.add("A", "B", "C", "D")
        net.connect("A", "B"); net.connect("B", "C"); net.connect("A", "D"); net.connect("D", "C")
        net.duplicateFrames = true; net.discover()
        net.nodes.getValue("A").originate(node("C"), SosCodec.encode(incident()), 3); net.run()
        assertEquals(1, observations)
        val server = FakeLabServer(); confirm(g, server)
        assertEquals(1, server.requests.size)
    }
    @Test fun retryWithNewMeshIdPreservesIncidentAndDoesNotCreateAnotherHttpDelivery() {
        val clock = FakeClock(); val g = SosInternetGateway(clock).also { it.enable(labEndpoint) }
        val seen = mutableListOf<String>(); val net = chain(g, seen)
        val text = SosCodec.encode(incident()); val a = net.nodes.getValue("A")
        a.originate(node("D"), text, 2, id("attempt1")); net.run()
        val server = FakeLabServer(); confirm(g, server)
        a.originate(node("D"), text, 2, id("attempt2")); net.run()
        assertEquals(2, seen.size); assertEquals(GatewayAdmission.DUPLICATE, g.snapshot().lastAdmission)
        clock.time = 5000; assertNull(g.beginDelivery(true)); assertEquals(1, server.requests.size)
    }
    @Test fun conflictingUuidFromAnotherMeshAttemptIsRejectedWithoutReplacingOriginal() {
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }; val net = chain(g)
        val a = net.nodes.getValue("A")
        a.originate(node("D"), SosCodec.encode(incident()), 2); net.run()
        a.originate(node("D"), SosCodec.encode(incident().copy(severity = DeclaredSeverity.CRITICAL)), 2); net.run()
        assertEquals(GatewayAdmission.CONFLICT, g.snapshot().lastAdmission)
        val server = FakeLabServer(); confirm(g, server)
        assertEquals(DeclaredSeverity.HIGH, server.requests.single().payload.severity)
    }
    @Test fun disabledGatewayDoesNotObservePastHistoryWhenLaterEnabled() {
        val g = SosInternetGateway(FakeClock()); val net = chain(g)
        net.nodes.getValue("A").originate(node("C"), SosCodec.encode(incident()), 2); net.run()
        assertEquals(1, net.received("C").size)
        assertEquals(0, g.snapshot().tracked)
        g.enable(labEndpoint); assertNull(g.beginDelivery(true)); assertEquals(0, g.snapshot().tracked)
    }
    @Test fun privateAndMalformedInboundSosNeverReachObserverOrQueue() {
        var observed = 0
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }
        val receiver = MeshBleNode(node("C"), FakeClock(), MeshGattPort { true },
            acceptedInbound = { observed++; g.onAcceptedMesh(it) })
        receiver.setEnabled(true)
        val session = session(); receiver.openIncoming(session)
        receiver.acceptIdentity(session, MeshLinkProtocol.hello(node("B")))
        val p = SosMeshPolicy.SYNTHETIC_POSITION
        val content = listOf(SosCodec.encode(incident(p.copy(source = PositionSource.GNSS))),
            SosCodec.encode(incident(p.copy(source = PositionSource.MOCK_GPS))),
            SosCodec.encode(incident(p.copy(latitude = -51.6))), "BCSOS:broken")
        content.forEachIndexed { index, text ->
            val message = MeshMessage(id("bad$index"), node("A"), null, text, 2)
            val frames = MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message), index + 1)
            frames.dropLast(1).forEach { assertTrue(receiver.acceptFrame(session, it)) }
            assertFalse(receiver.acceptFrame(session, frames.last()))
        }
        assertEquals(0, observed); assertEquals(0, g.snapshot().tracked)
        assertTrue(receiver.snapshot().events.none { it.message != null })
    }
    @Test fun engineRejectionsAndDuplicateAttemptsNeverReachObserver() {
        var observed = 0
        val receiver = MeshBleNode(node("C"), FakeClock(), MeshGattPort { true }, acceptedInbound = { observed++ })
        receiver.setEnabled(true); val session = session(); receiver.openIncoming(session)
        receiver.acceptIdentity(session, MeshLinkProtocol.hello(node("B")))
        val accepted = MeshMessage(id("same"), node("A"), null, SosCodec.encode(incident()), 2)
        val own = accepted.copy(originId = node("C"), messageId = id("own"))
        for ((index, message) in listOf(accepted, accepted, own).withIndex()) {
            MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message), index + 1).forEach { receiver.acceptFrame(session, it) }
        }
        assertEquals(1, observed)
    }
    @Test fun chatAndLegacyProtocolStillWorkWithGatewayEnabledAndObserverFailureDoesNotBreakRelay() {
        val g = SosInternetGateway(FakeClock()).also { it.enable(labEndpoint) }; val net = chain(g)
        net.nodes.getValue("A").originate(node("C"), "Chat normal 🆘", 2); net.run()
        assertEquals("Chat normal 🆘", net.received("C").single().content)
        assertEquals(0, g.snapshot().tracked); assertNull(g.beginDelivery(true))
        assertEquals("Hola", DirectBleProtocol.decode(DirectBleProtocol.encode("ALFA", "Hola")).message)
        val failing = FakeGattNetwork { _, _ -> throw IllegalStateException("observer") }
        failing.add("A", "B", "C"); failing.connect("A", "B"); failing.connect("B", "C"); failing.discover()
        failing.nodes.getValue("A").originate(node("C"), "Chat", 2); failing.run()
        assertEquals("Chat", failing.received("C").single().content)
    }
}
