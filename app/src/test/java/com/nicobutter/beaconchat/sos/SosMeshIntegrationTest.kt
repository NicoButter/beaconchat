package com.nicobutter.beaconchat.sos

import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import com.nicobutter.beaconchat.mesh.ble.*
import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import org.junit.Assert.*
import org.junit.Test

class SosMeshIntegrationTest {
    private fun chain(): FakeGattNetwork {
        val net = FakeGattNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C"); net.discover()
        return net
    }
    @Test fun syntheticAndAbsentPositionSurviveEntireWirePathWithUtf8Notes() {
        for (position in listOf(null, SosMeshPolicy.SYNTHETIC_POSITION)) {
            val sos = incident(position, "sí 🆘 ".repeat(20))
            val original = MeshMessage(id("attempt"), node("A"), node("B"), SosCodec.encode(sos), 1)
            val encoded = MeshMessageCodec.encode(original)
            val frames = MeshFragmentProtocol.fragment(encoded, 42).reversed()
            val receiver = MeshReassembler(FakeClock())
            var complete: ByteArray? = null
            frames.forEach { frame ->
                assertTrue(frame.size <= 20)
                val result = receiver.accept(session(), frame)
                if (result is ReassemblyResult.Complete) complete = result.payload
                else assertEquals(ReassemblyResult.Duplicate, receiver.accept(session(), frame.copyOf()))
            }
            val message = MeshMessageCodec.decode(complete!!)
            assertEquals(original, message)
            assertEquals(sos, SosCodec.decode(message.content))
        }
    }
    @Test fun actualBleAdapterPolicyRelaysSyntheticSosAcrossAbcWithoutAc() {
        val net = chain()
        val sos = SosMeshPolicy.laboratoryIncident(true, EmergencyType.TRAPPED, DeclaredSeverity.CRITICAL, 1234)
        val gateway = SosMeshGateway { content -> net.nodes.getValue("A").originate(node("C"), content, 2) }
        assertEquals(SosSubmissionStatus.ORIGINATED_LOCALLY, gateway.submit(sos).status)
        assertTrue(net.received("C").isEmpty()) // Local acceptance is not destination receipt.
        net.run()
        val received = net.received("C").single()
        assertEquals(sos, SosCodec.decode(received.content))
        assertEquals(node("A"), received.originId)
        assertEquals(0, received.hopsRemaining)
        assertFalse("A" to "C" in net.established)
    }
    @Test fun retriesPreserveIncidentUuidCreationAndBytesWhileAttemptsHaveDistinctMeshIds() {
        val net = chain()
        val sos = SosMeshPolicy.laboratoryIncident(false, EmergencyType.MEDICAL, DeclaredSeverity.HIGH, 999)
        val gateway = SosMeshGateway { content -> net.nodes.getValue("A").originate(node("C"), content, 2) }
        repeat(2) { assertEquals(sos.incidentId, gateway.submit(sos).incidentId); net.run() }
        val received = net.received("C")
        assertEquals(2, received.size)
        assertNotEquals(received[0].messageId, received[1].messageId)
        assertEquals(received[0].content, received[1].content)
        assertTrue(received.all { SosCodec.decode(it.content) == sos })
    }
    @Test fun privateGpsAndMockCoordinatesNeverInvokeMeshOrigination() {
        var invoked = 0
        val gateway = SosMeshGateway { invoked++; true }
        for (source in listOf(PositionSource.GNSS, PositionSource.MOCK_GPS)) {
            val private = incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(source = source))
            assertEquals(SosSubmissionStatus.PRIVACY_BLOCKED, gateway.submit(private).status)
        }
        assertEquals(0, invoked)
    }
    @Test fun syntheticTagAloneCannotLaunderArbitraryCoordinatesAndRealIncidentsStayLocal() {
        assertFalse(SosMeshPolicy.permitted(incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(latitude = -51.6))))
        assertFalse(SosMeshPolicy.permitted(incident(laboratory = false)))
        assertTrue(SosMeshPolicy.permitted(incident()))
        assertTrue(SosMeshPolicy.permitted(incident(SosMeshPolicy.SYNTHETIC_POSITION)))
        val local = incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(source = PositionSource.GNSS), "Nota local privada", false)
        val fixture = SosMeshPolicy.laboratoryIncident(true, local.type, local.severity, local.createdAtEpochMillis)
        assertNotEquals(local.incidentId, fixture.incidentId)
        assertNotEquals(local.note, fixture.note)
        assertNotEquals(local.position, fixture.position)
    }
    @Test fun meshOriginBoundaryRejectsPrivateSosEvenWhenBypassingTypedGateway() {
        val net = chain()
        val private = incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(source = PositionSource.GNSS))
        val serialized = SosCodec.encode(private)
        assertFalse(net.nodes.getValue("A").originate(null, serialized, 3))
        net.run()
        assertTrue(net.received("B").isEmpty())
        assertTrue(net.written("A").isEmpty())
        assertTrue(net.nodes.getValue("A").snapshot().events.none { it.message?.content == serialized })
    }
    @Test fun inboundPrivacyBoundaryPreventsLocalDeliveryLoggingAndRelayOfPrivateSos() {
        val receiver = MeshBleNode(node("B"), FakeClock(), MeshGattPort { true })
        receiver.setEnabled(true); receiver.openIncoming(session())
        receiver.acceptIdentity(session(), MeshLinkProtocol.hello(node("A")))
        val sos = incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(source = PositionSource.GNSS))
        val message = MeshMessage(id("private"), node("A"), null, SosCodec.encode(sos), 3)
        val frames = MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message), 1)
        frames.dropLast(1).forEach { assertTrue(receiver.acceptFrame(session(), it)) }
        assertFalse(receiver.acceptFrame(session(), frames.last()))
        assertTrue(receiver.snapshot().events.none { it.message != null })
        assertEquals(0, receiver.snapshot().queued)
        assertEquals(0, receiver.reassemblySnapshot().bufferedBytes)
    }
    @Test fun malformedReservedPayloadIsRejectedAndNormalChatsAreUnchanged() {
        val net = chain()
        val a = net.nodes.getValue("A")
        assertFalse(a.originate(null, "BCSOS:invalid", 3))
        val text = "Mensaje normal: sí 🆘"
        assertTrue(a.originate(node("C"), text, 2)); net.run()
        assertEquals(text, net.received("C").single().content)
        assertTrue(SosMeshPolicy.contentPermitted(text))
        val legacy = DirectBleProtocol.encode("ALFA", "sí:🆘")
        assertEquals("sí:🆘", DirectBleProtocol.decode(legacy).message)
        rejectsSos { SosCodec.decode(text) }
    }
    @Test fun disabledOrUnavailableMeshNeverInventsQueueOrFinalDelivery() {
        val a = MeshBleNode(node("A"), FakeClock(), MeshGattPort { true })
        val sos = incident()
        val gateway = SosMeshGateway { a.originate(null, it, 3) }
        assertEquals(SosSubmissionStatus.REJECTED, gateway.submit(sos).status)
        a.setEnabled(true)
        assertEquals(SosSubmissionStatus.ORIGINATED_LOCALLY, gateway.submit(sos).status)
        assertEquals(0, a.snapshot().queued)
        assertTrue(a.snapshot().events.none { it.kind == MeshActivityKind.QUEUED || it.kind == MeshActivityKind.LINK_WRITTEN })
        assertTrue(a.snapshot().events.any { it.kind == MeshActivityKind.FAILURE })
    }
}
