package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.core.*
import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque

/** Real wire codec/fragmentation/reassembly on every simulated link; no Android or Bluetooth. */
private class WireNetwork {
    data class Transmission(val from: NodeId, val to: NodeId, val frames: List<ByteArray>)
    val clock = FakeClock()
    val engines = linkedMapOf<NodeId, MeshEngine>()
    val receivers = linkedMapOf<NodeId, MeshReassembler>()
    val links = linkedMapOf<NodeId, MutableSet<NodeId>>()
    val deliveries = linkedMapOf<NodeId, MutableList<MeshMessage>>()
    val decisions = mutableListOf<Pair<NodeId, MeshDecision>>()
    val transmissions = mutableListOf<Transmission>()
    val dispatches = mutableListOf<DispatchResult>()
    val handoffs = mutableListOf<Pair<NodeId, MeshMessage>>()
    val rejections = mutableListOf<FragmentRejection>()
    var alterFrames: (NodeId, NodeId, List<ByteArray>) -> List<ByteArray> = { _, _, frames -> frames }
    private val inFlight = ArrayDeque<Transmission>()
    private val sessions = mutableMapOf<Pair<NodeId, NodeId>, MeshLinkSession>()
    private var nextSession = 0L
    private var nextTransfer = 0

    fun add(vararg names: String) = names.forEach {
        val id = node(it)
        engines[id] = MeshEngine(id, clock)
        receivers[id] = MeshReassembler(clock)
        links[id] = linkedSetOf()
        deliveries[id] = mutableListOf()
    }

    fun connect(a: String, b: String) {
        links.getValue(node(a)).add(node(b))
        links.getValue(node(b)).add(node(a))
    }

    fun publish(from: String, to: String?, text: String, ttl: Int, messageId: MessageId = id(text)): MeshMessage {
        val decision = engines.getValue(node(from)).originate(to?.let(::node), text, ttl, messageId)
        record(node(from), decision)
        return decision.message
    }

    fun reconnect(from: String, to: String) {
        val edge = node(from) to node(to)
        sessions.remove(edge)?.let { receivers.getValue(node(to)).discardSession(it) }
    }

    private fun record(at: NodeId, result: MeshDecision) {
        decisions.add(at to result)
        if (result.deliverLocally) deliveries.getValue(at).add(result.message)
    }

    fun runUntilIdle() {
        repeat(30) {
            for ((sender, engine) in engines) {
                dispatches += engine.dispatch(MeshTransport { message, excluded ->
                    handoffs.add(sender to message)
                    val targets = links.getValue(sender).filter { it !in excluded }
                    targets.forEach { target ->
                        val encoded = MeshMessageCodec.encode(message)
                        val frames = MeshFragmentProtocol.fragment(encoded, nextTransfer++)
                        assertTrue(frames.all { it.size <= 20 })
                        val transmission = Transmission(sender, target, alterFrames(sender, target, frames))
                        transmissions.add(transmission)
                        check(transmissions.size <= 50) { "Unbounded network forwarding" }
                        inFlight.addLast(transmission)
                    }
                    if (targets.isEmpty()) Handoff.UNAVAILABLE else Handoff.ACCEPTED
                })
            }
            while (inFlight.isNotEmpty()) {
                val transmission = inFlight.removeFirst()
                val session = sessions.getOrPut(transmission.from to transmission.to) { session(++nextSession) }
                for (frame in transmission.frames) {
                    when (val result = receivers.getValue(transmission.to).accept(session, frame)) {
                        is ReassemblyResult.Complete -> record(transmission.to,
                            engines.getValue(transmission.to).receive(MeshMessageCodec.decode(result.payload), transmission.from))
                        is ReassemblyResult.Rejected -> rejections.add(result.reason)
                        else -> Unit
                    }
                }
            }
            if (engines.values.all { it.snapshot().pendingMessages == 0 }) return
        }
        error("Network did not become idle")
    }
}

class MeshTransportIntegrationTest {
    @Test fun twoNodesExchangeMaximumUtf8AndShortReplyWithIntactEnvelopes() {
        val net = WireNetwork()
        net.add("A", "B"); net.connect("A", "B")
        val request = net.publish("A", "B", "🆘".repeat(256), 1)
        val reply = net.publish("B", "A", "sí", 1)
        net.runUntilIdle()
        assertEquals(request.copy(hopsRemaining = 0), net.deliveries.getValue(node("B")).single())
        assertEquals(reply.copy(hopsRemaining = 0), net.deliveries.getValue(node("A")).single())
        assertEquals(135, net.transmissions.first().frames.size)
        assertTrue(net.rejections.isEmpty())
    }

    @Test fun chainReassemblesBeforeForwardingAndDecrementsTtlOncePerLogicalHop() {
        val net = WireNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C")
        net.alterFrames = { _, _, frames -> frames.reversed().flatMap { listOf(it, it.copyOf()) } }
        val original = net.publish("A", "C", "á".repeat(512), 2)
        net.runUntilIdle()
        assertEquals(listOf(node("A"), node("B")), net.transmissions.map { it.from })
        assertEquals(original.copy(hopsRemaining = 0), net.deliveries.getValue(node("C")).single())
        assertTrue(net.deliveries.getValue(node("B")).isEmpty())
        assertEquals(listOf(1, 0), net.dispatches.map { it.message.hopsRemaining })
        assertTrue(net.rejections.all { it == FragmentRejection.CLOSED_TRANSFER })
    }

    @Test fun broadcastInRingDeliversOnceAndTerminatesDespiteFragmentDuplicates() {
        val net = WireNetwork()
        val names = listOf("A", "B", "C", "D", "E", "F")
        net.add(*names.toTypedArray())
        names.indices.forEach { net.connect(names[it], names[(it + 1) % names.size]) }
        net.alterFrames = { _, _, frames -> frames.flatMap { listOf(it, it.copyOf()) } }
        net.publish("A", null, "Broadcast: sí 🆘", 16)
        net.runUntilIdle()
        names.forEach { assertEquals(1, net.deliveries.getValue(node(it)).size) }
        assertTrue(net.transmissions.size <= 12)
        assertTrue(net.engines.values.all { it.snapshot().pendingMessages == 0 })
    }

    @Test fun diamondDeduplicatesLogicalMessagesArrivingViaDifferentTransfersAndRelays() {
        val net = WireNetwork()
        net.add("A", "B", "C", "D", "E")
        net.connect("A", "B"); net.connect("A", "C"); net.connect("B", "D")
        net.connect("C", "D"); net.connect("D", "E")
        val original = net.publish("A", "E", "Dos caminos: 🆘".repeat(30), 4)
        net.runUntilIdle()
        assertEquals(1, net.handoffs.count { it.first == node("D") })
        assertEquals(1, net.transmissions.count { it.from == node("D") && it.to == node("E") })
        assertEquals(original.copy(hopsRemaining = 1), net.deliveries.getValue(node("E")).single())
        assertEquals(1, net.decisions.count { it.first == node("D") && it.second.acceptance == Acceptance.DUPLICATE })
    }

    @Test fun exhaustedTtlDoesNotReachThirdNodeAndZeroNeverCreatesFrames() {
        val net = WireNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C")
        net.publish("A", "C", "Local only", 0)
        net.runUntilIdle()
        assertTrue(net.transmissions.isEmpty())
        net.publish("A", "C", "Un salto", 1)
        net.runUntilIdle()
        assertEquals(1, net.transmissions.size)
        assertTrue(net.deliveries.getValue(node("C")).isEmpty())
        assertEquals(Forwarding.TTL_EXHAUSTED, net.decisions.last().second.forwarding)
    }

    @Test fun disconnectedChainReportsUnavailableWithoutInventingDelivery() {
        val net = WireNetwork()
        net.add("A", "B", "C"); net.connect("A", "B")
        net.publish("A", "C", "Sin enlace", 3)
        net.runUntilIdle()
        assertEquals(DispatchStatus.UNAVAILABLE, net.dispatches.last().status)
        assertTrue(net.deliveries.getValue(node("C")).isEmpty())
    }

    @Test fun missingFragmentPreventsAnyReceiverEngineHandoffAndExpires() {
        val net = WireNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C")
        net.alterFrames = { _, _, frames -> frames.filterIndexed { index, _ -> index != 2 } }
        net.publish("A", "C", "Ayuda larga 🆘".repeat(40), 2)
        net.runUntilIdle()
        assertEquals(1, net.transmissions.size)
        assertTrue(net.decisions.none { it.first == node("B") || it.first == node("C") })
        assertEquals(0, net.engines.getValue(node("B")).snapshot().seenMessages)
        net.clock.time = 15_000
        assertEquals(1, net.receivers.getValue(node("B")).cleanup())
        assertEquals(0, net.receivers.getValue(node("B")).snapshot().bufferedBytes)
    }

    @Test fun payloadCorruptionNeverReachesAnEngine() {
        val net = WireNetwork()
        net.add("A", "B"); net.connect("A", "B")
        net.alterFrames = { _, _, frames -> frames.also { it[2][12] = (it[2][12].toInt() xor 1).toByte() } }
        net.publish("A", "B", "Integridad", 1)
        net.runUntilIdle()
        assertEquals(listOf(FragmentRejection.INTEGRITY_FAILED), net.rejections)
        assertTrue(net.deliveries.getValue(node("B")).isEmpty())
        assertEquals(0, net.engines.getValue(node("B")).snapshot().seenMessages)
    }

    @Test fun logicalOriginsAndRetryIdentitySurviveFreshLinkSessions() {
        val net = WireNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("C", "B")
        val sharedId = id("same message id, independent origins")
        net.publish("A", "B", "Desde A", 1, sharedId)
        net.publish("C", "B", "Desde C", 1, sharedId)
        net.runUntilIdle()
        net.reconnect("A", "B") // A new BLE incarnation need not change the logical NodeId.
        val retry = net.deliveries.getValue(node("B")).first()
        val bytes = MeshMessageCodec.encode(retry)
        val receiver = net.receivers.getValue(node("B"))
        val result = MeshFragmentProtocol.fragment(bytes, 42).map { receiver.accept(session(999), it) }.last()
        val decision = net.engines.getValue(node("B")).receive(
            MeshMessageCodec.decode((result as ReassemblyResult.Complete).payload), node("A"))
        assertEquals(Acceptance.DUPLICATE, decision.acceptance)
        assertEquals(listOf("Desde A", "Desde C"), net.deliveries.getValue(node("B")).map { it.content })
        assertNotEquals(net.deliveries.getValue(node("B"))[0].originId, net.deliveries.getValue(node("B"))[1].originId)
    }

    @Test fun validCrcCannotMakeMalformedEnvelopeOrUnsupportedVersionReachEngine() {
        val engine = MeshEngine(node("receiver"), FakeClock())
        val receiver = MeshReassembler(FakeClock())
        val valid = MeshMessageCodec.encode(message())
        val invalid = listOf(valid.copyOf().also { it[0] = 2 }, valid + 0,
            valid.copyOf().also { it[it.lastIndex] = 0xff.toByte() })
        invalid.forEachIndexed { index, bytes ->
            val result = MeshFragmentProtocol.fragment(bytes, index).map { receiver.accept(session(), it) }.last()
            assertTrue(result is ReassemblyResult.Complete) // Integrity is separate from envelope validation.
            rejects { engine.receive(MeshMessageCodec.decode((result as ReassemblyResult.Complete).payload), node("sender")) }
        }
        assertEquals(MeshSnapshot(0, 0), engine.snapshot())
    }
}
