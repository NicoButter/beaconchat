package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import com.nicobutter.beaconchat.sos.*
import org.junit.Assert.*
import org.junit.Test

/** Deterministic callback delays; these measurements are NOT Android/radio benchmarks. */
class MeshBleTimingTest {
    private class TimedLink(val content: String, val operationMillis: Long = 100,
        val diagnostics: MutableList<MeshDiagnostic> = mutableListOf()) {
        val clock = FakeClock()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true }, diagnostics = MeshDiagnostics(diagnostics::add))
        val commands = java.util.ArrayDeque<MeshGattCommand>()
        val results = mutableListOf<MeshLinkResult>()
        val client = MeshGattClient(node("A"), clock, MeshGattPort { commands.add(it); true },
            results::add, MeshDiagnostics(diagnostics::add))
        val session = session()
        val message = message().copy(content = content, destinationId = node("B"), hopsRemaining = 0)
        val encoded = MeshMessageCodec.encode(message)
        val count = MeshFragmentProtocol.fragment(encoded, 42).size

        fun run() {
            receiver.setEnabled(true)
            client.start(MeshGattJob(1, "B", node("B"), message, encoded, 42))
            var operations = 0
            while (commands.isNotEmpty()) {
                check(operations++ < 400)
                val command = commands.removeFirst()
                if (command is MeshGattCommand.Close) { receiver.closeIncoming(session); continue }
                clock.time += operationMillis
                client.tick(); receiver.tick()
                if (!client.busy) continue
                when (command) {
                    is MeshGattCommand.Connect -> client.connected(1, receiver.openIncoming(session))
                    is MeshGattCommand.Discover -> client.services(1, true)
                    is MeshGattCommand.ReadIdentity -> client.identityRead(1, receiver.readIdentity(session) ?: byteArrayOf(), true)
                    is MeshGattCommand.WriteIdentity -> client.identityWritten(1, receiver.acceptIdentity(session, command.value))
                    is MeshGattCommand.WriteFrame -> client.frameWritten(1, receiver.acceptFrame(session, command.value))
                    is MeshGattCommand.ReadReceipt -> client.receiptRead(1, receiver.readReceipt(session) ?: byteArrayOf(), true)
                    else -> Unit
                }
            }
        }
    }

    @Test fun shortTypicalSosMaximumSosAnd1024BytesCompleteWithMeasuredMetadata() {
        val fixtures = java.util.Properties().apply {
            MeshBleTimingTest::class.java.getResourceAsStream("/mesh-ble-fixtures.properties")!!.use(::load)
        }
        for (key in listOf("sos_typical", "sos_maximum")) {
            val content = fixtures.getProperty(key)
            val incident = SosCodec.decode(content)
            assertTrue(SosMeshPolicy.permitted(incident))
            assertEquals(SosMeshPolicy.SYNTHETIC_POSITION, incident.position)
            assertEquals(content, SosCodec.encode(incident))
        }
        assertEquals(256, SosCodec.decode(fixtures.getProperty("sos_maximum")).note!!.toByteArray().size)
        assertEquals(1024, fixtures.getProperty("mesh_1024").toByteArray().size)
        val cases = listOf("short", "sos_typical", "sos_maximum", "mesh_1024").map { it to fixtures.getProperty(it) }
        cases.forEach { (label, content) ->
            val link = TimedLink(content)
            link.run()
            assertEquals(label, MeshLinkOutcome.LINK_WRITTEN, link.results.single().outcome)
            assertEquals(content, link.receiver.snapshot().events.single { it.kind == MeshActivityKind.LOCAL_RECEIVED }.message!!.content)
            val result = link.diagnostics.single { it.event == "gatt_result" }
            assertEquals(link.count * 200L, result.durationMillis)
            assertEquals(400 + link.count * 200L, result.elapsedMillis)
            assertEquals(4 + link.count * 2, link.diagnostics.count { it.event == "gatt_operation" })
            assertEquals(link.count, link.diagnostics.count { it.event == "incoming_fragment" })
            assertEquals(1, link.diagnostics.count { it.event == "incoming_fragment" && it.status == "COMPLETE" })
            assertEquals(0, link.receiver.reassemblySnapshot().bufferedBytes)
            assertEquals(0, link.receiver.snapshot().incoming)
            println("SIMULATED $label contentBytes=${content.toByteArray().size} envelopeBytes=${link.encoded.size} " +
                "frames=${link.count} operationMs=100 transferMs=${result.durationMillis} totalMs=${result.elapsedMillis}")
        }
    }

    @Test fun maximumFitsAt150MillisPerOperationWhileOld14SecondsCannotFit100Millis() {
        val link = TimedLink("x".repeat(1024), 150)
        assertEquals(135, link.count)
        assertTrue(link.count * 2 * 100 > MeshBleTiming.LEGACY_TRANSFER_MILLIS)
        link.run()
        assertEquals(MeshLinkOutcome.LINK_WRITTEN, link.results.single().outcome)
        assertEquals(40_500L, link.diagnostics.single { it.event == "gatt_result" }.durationMillis)
        assertTrue(MeshBleTiming.MAX_TRANSFER_MILLIS < MeshBleTiming.REASSEMBLY_MILLIS)
        assertTrue(MeshBleTiming.JOB_MILLIS < MeshBleTiming.INCOMING_SESSION_MILLIS)
    }

    @Test fun progressCannotKeepMaximumTransferAliveIndefinitely() {
        val link = TimedLink("x".repeat(1024), 200)
        link.run()
        assertEquals(MeshLinkOutcome.FAILED, link.results.single().outcome)
        assertTrue(link.diagnostics.any { it.event == "gatt_timeout" && it.status == "TRANSFER" })
        assertTrue(link.receiver.snapshot().events.none { it.kind == MeshActivityKind.LOCAL_RECEIVED })
        assertEquals(0, link.receiver.reassemblySnapshot().bufferedBytes)
    }

    @Test fun receiverRetentionIsAbsoluteAndDoesNotGrowWithProgress() {
        val clock = FakeClock()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true })
        receiver.setEnabled(true); receiver.openIncoming(session())
        receiver.acceptIdentity(session(), MeshLinkProtocol.hello(node("A")))
        val frames = MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message().copy(content = "x".repeat(1024))), 1)
        assertTrue(receiver.acceptFrame(session(), frames[0]))
        clock.time = 19_000; assertTrue(receiver.acceptFrame(session(), frames[1]))
        clock.time = 38_000; assertTrue(receiver.acceptFrame(session(), frames[2]))
        clock.time = MeshBleTiming.REASSEMBLY_MILLIS
        receiver.tick()
        assertEquals(0, receiver.reassemblySnapshot().bufferedBytes)
        assertTrue(receiver.snapshot().events.none { it.kind == MeshActivityKind.LOCAL_RECEIVED })
    }

    @Test fun lateSuccessWithoutTickerCannotOverrideAnOperationDeadline() {
        val link = TimedLink("HOLA")
        link.client.start(MeshGattJob(1, "B", node("B"), link.message, link.encoded, 1))
        link.clock.time = MeshBleTiming.OPERATION_MILLIS
        link.client.connected(1, true)
        assertFalse(link.client.busy)
        assertEquals(MeshLinkOutcome.FAILED, link.results.single().outcome)
        assertTrue(link.commands.none { it is MeshGattCommand.Discover })
    }

    @Test fun diagnosticsContainNoPayloadAndObserverFailuresCannotBreakDelivery() {
        val content = "PRIVATE_NOTE_SECRET_TOKEN_123456789"
        val link = TimedLink(content); link.run()
        assertFalse(link.diagnostics.joinToString().contains(content))
        val receiver = MeshBleNode(node("B"), FakeClock(), MeshGattPort { true },
            diagnostics = MeshDiagnostics { throw IllegalStateException("observer") })
        receiver.setEnabled(true); receiver.openIncoming(session())
        assertTrue(receiver.acceptIdentity(session(), MeshLinkProtocol.hello(node("A"))))
        MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message().copy(content = content, destinationId = node("B"))), 1).forEach {
            assertTrue(receiver.acceptFrame(session(), it))
        }
        assertEquals(content, receiver.snapshot().events.single { it.kind == MeshActivityKind.LOCAL_RECEIVED }.message!!.content)
    }
}
