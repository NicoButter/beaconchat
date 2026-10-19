package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import org.junit.Assert.*
import org.junit.Test

class MeshGattClientTest {
    private val clock = FakeClock()
    private val commands = mutableListOf<MeshGattCommand>()
    private val results = mutableListOf<MeshLinkResult>()
    private var reject: (MeshGattCommand) -> Boolean = { false }
    private val client = MeshGattClient(node("A"), clock, MeshGattPort { commands.add(it); !reject(it) }, results::add)
    private fun job(token: Long = 1, probe: Boolean = false) = MeshGattJob(token, "radio-B", node("B"),
        if (probe) null else message(), if (probe) null else MeshMessageCodec.encode(message()), 42)
    private fun handshake(token: Long = 1) {
        client.connected(token, true)
        client.services(token, true)
        client.identityRead(token, MeshLinkProtocol.hello(node("B")), true)
        client.identityWritten(token, true)
    }
    private fun currentFrame() = (commands.last() as MeshGattCommand.WriteFrame).value

    @Test fun writesOnlyAfterHandshakeAndEachWriteAndReadCallback() {
        client.start(job())
        assertTrue(commands.last() is MeshGattCommand.Connect)
        handshake()
        var count = 0
        while (client.busy) {
            val frame = currentFrame()
            val before = commands.size
            client.frameWritten(99, true)
            assertEquals(before, commands.size)
            client.frameWritten(1, true)
            assertTrue(commands.last() is MeshGattCommand.ReadReceipt)
            client.receiptRead(1, MeshLinkProtocol.receipt(frame), true)
            count++
        }
        assertEquals(MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 42).size, count)
        assertEquals(listOf(MeshLinkOutcome.LINK_WRITTEN), results.map { it.outcome })
        assertTrue(results.single().detail.contains("destino final sin confirmar"))
        assertTrue(commands.last() is MeshGattCommand.Close)
    }
    @Test fun duplicateCallbacksCannotSkipFramesOrFinishTwice() {
        client.start(job()); handshake()
        val first = currentFrame()
        client.frameWritten(1, true)
        val size = commands.size
        client.frameWritten(1, true)
        client.identityWritten(1, true)
        assertEquals(size, commands.size)
        client.receiptRead(1, MeshLinkProtocol.receipt(first), true)
        val second = currentFrame()
        client.receiptRead(1, MeshLinkProtocol.receipt(first), true)
        assertArrayEquals(second, currentFrame())
        client.frameWritten(1, true)
        client.receiptRead(1, MeshLinkProtocol.receipt(first), true) // Old receipt while expecting the next one.
        assertEquals(MeshGattClient.Phase.READING_RECEIPT, client.phase)
        assertTrue(results.isEmpty())
        client.receiptRead(1, MeshLinkProtocol.receipt(second), true)
        assertEquals(MeshGattClient.Phase.WRITING_FRAME, client.phase)
    }
    @Test fun delayedWriteCallbackStillNeedsReceiptForCurrentFrame() {
        client.start(job()); handshake()
        val old = currentFrame()
        client.frameWritten(1, true); client.receiptRead(1, MeshLinkProtocol.receipt(old), true)
        client.frameWritten(1, true) // Could be a duplicated callback for the previous write.
        client.receiptRead(1, MeshLinkProtocol.receipt(old), true)
        assertTrue(results.isEmpty())
        clock.time = 5_000; client.tick()
        assertEquals(MeshLinkOutcome.FAILED, results.single().outcome)
    }
    @Test fun staleConnectionCallbacksDoNotAffectNewJob() {
        client.start(job()); client.cancel()
        client.start(job(2)); handshake(2)
        val size = commands.size
        client.connected(1, true); client.services(1, true); client.identityRead(1, MeshLinkProtocol.hello(node("B")), true)
        client.identityWritten(1, true); client.frameWritten(1, true); client.receiptRead(1, byteArrayOf(), true); client.disconnected(1)
        assertEquals(size, commands.size)
        assertEquals(2L, client.job!!.token)
        assertEquals(1, results.size)
    }
    @Test fun malformedUnsupportedChangedAndOwnIdentitiesFailBeforeAnyFrame() {
        val values = listOf(byteArrayOf(), MeshLinkProtocol.hello(node("B")).also { it[2] = 2 },
            MeshLinkProtocol.hello(node("C")), MeshLinkProtocol.hello(node("A")))
        values.forEachIndexed { index, bytes ->
            val token = index.toLong() + 1
            client.start(job(token)); client.connected(token, true); client.services(token, true)
            client.identityRead(token, bytes, true)
            assertFalse(client.busy)
        }
        assertTrue(commands.none { it is MeshGattCommand.WriteFrame })
        assertTrue(results.all { it.outcome == MeshLinkOutcome.FAILED })
    }
    @Test fun probeVerifiesBothIdentitiesWithoutSendingMessageFrames() {
        client.start(job(probe = true)); handshake()
        assertEquals(MeshLinkOutcome.PROBED, results.single().outcome)
        assertEquals(node("B"), results.single().remote)
        assertTrue(commands.none { it is MeshGattCommand.WriteFrame })
    }
    @Test fun disconnectAndCancelAreTerminalAndExplicit() {
        client.start(job()); handshake(); client.disconnected(1); client.disconnected(1); client.cancel()
        assertEquals(1, results.size)
        assertEquals(MeshLinkOutcome.FAILED, results.single().outcome)
        client.start(job(2)); client.cancel(); client.disconnected(2)
        assertEquals(listOf(MeshLinkOutcome.FAILED, MeshLinkOutcome.CANCELLED), results.map { it.outcome })
    }
    @Test fun perOperationTimeoutCoversEveryWaitingPhase() {
        for (phase in MeshGattClient.Phase.values().filter { it != MeshGattClient.Phase.IDLE }) {
            clock.time = 0
            client.start(job())
            if (phase.ordinal >= MeshGattClient.Phase.DISCOVERING.ordinal) client.connected(1, true)
            if (phase.ordinal >= MeshGattClient.Phase.READING_IDENTITY.ordinal) client.services(1, true)
            if (phase.ordinal >= MeshGattClient.Phase.WRITING_IDENTITY.ordinal) client.identityRead(1, MeshLinkProtocol.hello(node("B")), true)
            if (phase.ordinal >= MeshGattClient.Phase.WRITING_FRAME.ordinal) client.identityWritten(1, true)
            if (phase == MeshGattClient.Phase.READING_RECEIPT) client.frameWritten(1, true)
            assertEquals(phase, client.phase)
            clock.time = 5_000; client.tick()
            assertFalse(client.busy)
        }
        assertEquals(6, results.size)
        assertTrue(results.all { it.outcome == MeshLinkOutcome.FAILED })
    }
    @Test fun slowProgressCannotExtendTransferPastReceiverRetention() {
        client.start(job()); handshake()
        repeat(4) {
            val frame = currentFrame()
            clock.time += 3_500
            client.frameWritten(1, true)
            client.receiptRead(1, MeshLinkProtocol.receipt(frame), true)
        }
        client.tick()
        assertFalse(client.busy)
        assertEquals(MeshLinkOutcome.FAILED, results.single().outcome)
    }
    @Test fun rejectedPortOperationOrReadWriteFailureNeverReportsLinkWritten() {
        reject = { it is MeshGattCommand.Connect }
        client.start(job())
        assertEquals(MeshLinkOutcome.FAILED, results.last().outcome)
        reject = { it is MeshGattCommand.WriteFrame }
        client.start(job(2)); handshake(2)
        assertEquals(MeshLinkOutcome.FAILED, results.last().outcome)
        reject = { false }
        client.start(job(3)); handshake(3); client.frameWritten(3, false)
        client.start(job(4)); handshake(4); client.frameWritten(4, true); client.receiptRead(4, byteArrayOf(), false)
        assertTrue(results.all { it.outcome == MeshLinkOutcome.FAILED })
    }
}
