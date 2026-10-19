package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque

/** Fake only replaces Bluetooth I/O; the production node, client, protocol and engine are real. */
internal class FakeGattNetwork {
    data class Connection(val target: String, val session: MeshLinkSession)
    val clock = FakeClock()
    val nodes = linkedMapOf<String, MeshBleNode>()
    val links = mutableSetOf<Pair<String, String>>()
    val commands = mutableListOf<Pair<String, MeshGattCommand>>()
    val connections = mutableMapOf<Pair<String, Long>, Connection>()
    val established = mutableListOf<Pair<String, String>>()
    val sessions = mutableListOf<MeshLinkSession>()
    val legacy = mutableSetOf<String>()
    var duplicateCallbacks = false
    var duplicateFrames = false
    var corruptFrom: String? = null
    var dropFrom: String? = null
    private val pending = ArrayDeque<() -> Unit>()
    private var nextSession = 0L

    fun add(vararg names: String) = names.forEach { name ->
        nodes[name] = MeshBleNode(node(name), clock, MeshGattPort { execute(name, it) })
        nodes.getValue(name).setEnabled(true)
    }
    fun connect(a: String, b: String) { links.add(a to b); links.add(b to a) }
    fun discover() { links.toList().forEach { (a, b) -> nodes[a]?.discovered(b) }; run() }
    fun run() {
        var count = 0
        while (pending.isNotEmpty()) {
            check(count++ < 20_000) { "Unbounded GATT work" }
            pending.removeFirst()()
        }
    }
    fun received(name: String) = nodes.getValue(name).snapshot().events.filter { it.kind == MeshActivityKind.LOCAL_RECEIVED }.map { it.message!! }
    fun written(name: String) = nodes.getValue(name).snapshot().events.filter { it.kind == MeshActivityKind.LINK_WRITTEN }
    fun failure(name: String) = nodes.getValue(name).snapshot().events.any { it.kind == MeshActivityKind.FAILURE }

    private fun execute(from: String, command: MeshGattCommand): Boolean {
        commands.add(from to command)
        val key = from to command.token
        if (command is MeshGattCommand.Close) {
            connections.remove(key)?.let { nodes[it.target]?.closeIncoming(it.session) }
            return true
        }
        pending.addLast {
            val client = nodes.getValue(from).client
            when (command) {
                is MeshGattCommand.Connect -> {
                    assertTrue(connections.keys.none { it.first == from }) // One outgoing GATT per node.
                    if (from to command.address !in links || (command.address !in nodes && command.address !in legacy)) {
                        client.connected(command.token, false)
                    } else {
                        val session = session(++nextSession)
                        if (command.address in legacy || nodes.getValue(command.address).openIncoming(session)) {
                            connections[key] = Connection(command.address, session)
                            sessions.add(session)
                            established.add(from to command.address)
                            client.connected(command.token, true)
                        } else client.connected(command.token, false)
                    }
                }
                is MeshGattCommand.Discover -> client.services(command.token, connections[key]?.target in nodes)
                is MeshGattCommand.ReadIdentity -> {
                    val connection = connections[key]
                    val value = connection?.let { nodes[it.target]?.readIdentity(it.session) }
                    client.identityRead(command.token, value ?: byteArrayOf(), value != null)
                }
                is MeshGattCommand.WriteIdentity -> {
                    val connection = connections[key]
                    client.identityWritten(command.token, connection?.let { nodes[it.target]?.acceptIdentity(it.session, command.value) } == true)
                }
                is MeshGattCommand.WriteFrame -> {
                    val connection = connections[key]
                    val bytes = command.value.copyOf()
                    assertTrue(bytes.size <= 20)
                    val index = bytes[4].toInt() and 0xff
                    val count = bytes[5].toInt() and 0xff
                    if (from == corruptFrom && index == 2) bytes[12] = (bytes[12].toInt() xor 1).toByte()
                    val admitted = if (from == dropFrom && index == 2) true // Lost frame with a misleading platform callback.
                        else connection?.let { nodes[it.target]?.acceptFrame(it.session, bytes) } == true
                    if (duplicateFrames && admitted && index < count - 1) {
                        assertTrue(nodes.getValue(connection!!.target).acceptFrame(connection.session, bytes.copyOf()))
                    }
                    client.frameWritten(command.token, admitted)
                    if (duplicateCallbacks) client.frameWritten(command.token, admitted)
                }
                is MeshGattCommand.ReadReceipt -> {
                    val connection = connections[key]
                    val value = connection?.let { nodes[it.target]?.readReceipt(it.session) }
                    client.receiptRead(command.token, value ?: byteArrayOf(), value != null)
                    if (duplicateCallbacks) client.receiptRead(command.token, value ?: byteArrayOf(), value != null)
                }
                else -> Unit
            }
        }
        return true
    }
}

class MeshBleAdapterTest {
    private fun chain(): FakeGattNetwork {
        val net = FakeGattNetwork()
        net.add("A", "B", "C"); net.connect("A", "B"); net.connect("B", "C"); net.discover()
        return net
    }

    @Test fun realAdapterPathRelaysAThroughBToCWithoutAnyDirectAcLink() {
        val net = chain()
        assertFalse("A" to "C" in net.links)
        val originalId = id("maximum UTF-8")
        assertTrue(net.nodes.getValue("A").originate(node("C"), "🆘".repeat(256), 2, originalId))
        net.run()
        val delivered = net.received("C").single()
        assertEquals(node("A"), delivered.originId)
        assertEquals(originalId, delivered.messageId)
        assertEquals(node("C"), delivered.destinationId)
        assertEquals("🆘".repeat(256), delivered.content)
        assertEquals(0, delivered.hopsRemaining)
        assertTrue(net.received("B").isEmpty())
        assertFalse("A" to "C" in net.established)
        assertEquals(1, net.written("A").size)
        assertEquals(1, net.written("B").size)
        assertTrue(net.connections.isEmpty())
        assertTrue(net.nodes.values.all { it.reassemblySnapshot().bufferedBytes == 0 && it.snapshot().incoming == 0 })
    }

    @Test fun removingBPreventsDestinationReceiptAndReportsFailure() {
        val net = chain()
        net.nodes.getValue("B").setEnabled(false)
        net.links.removeAll { it.first == "B" || it.second == "B" }
        net.nodes.getValue("A").originate(node("C"), "B desconectado", 2)
        net.run()
        assertTrue(net.received("C").isEmpty())
        assertTrue(net.written("A").isEmpty())
        assertTrue(net.failure("A"))
    }

    @Test fun fragmentAndCallbackDuplicatesNeverDuplicateDeliveryOrSkipWrites() {
        val net = chain()
        net.duplicateCallbacks = true; net.duplicateFrames = true
        net.nodes.getValue("A").originate(node("C"), "Duplicados 🆘".repeat(20), 2)
        net.run()
        assertEquals(1, net.received("C").size)
        assertEquals(1, net.written("A").size)
        assertEquals(1, net.written("B").size)
        assertFalse(net.failure("A"))
    }

    @Test fun diamondDeliversOnlyOnceAndDoesNotForwardBackToPreviousHop() {
        val net = FakeGattNetwork()
        net.add("A", "B", "C", "D", "E")
        net.connect("A", "B"); net.connect("A", "C"); net.connect("B", "D"); net.connect("C", "D"); net.connect("D", "E")
        net.discover()
        val start = net.established.size
        net.nodes.getValue("A").originate(node("E"), "Dos caminos", 4)
        net.run()
        assertEquals(1, net.received("E").size)
        val transfers = net.established.drop(start)
        assertEquals(1, transfers.count { it == "D" to "E" })
        assertFalse("B" to "A" in transfers)
        assertFalse("C" to "A" in transfers)
        assertTrue(transfers.size <= 8)
    }

    @Test fun broadcastRingTerminatesAndDeliversOncePerReceiver() {
        val net = FakeGattNetwork()
        net.add("A", "B", "C", "D")
        net.connect("A", "B"); net.connect("B", "C"); net.connect("C", "D"); net.connect("D", "A"); net.discover()
        val start = net.established.size
        net.nodes.getValue("A").originate(null, "Broadcast", 16)
        net.run()
        listOf("B", "C", "D").forEach { assertEquals(1, net.received(it).size) }
        assertTrue(net.received("A").isEmpty()) // The source has ORIGINATED, never a fabricated remote reception.
        assertTrue(net.established.size - start <= 8)
    }

    @Test fun exhaustedTtlStopsAtBAndZeroNeverStartsAnOutgoingLink() {
        val net = chain()
        val start = net.established.size
        net.nodes.getValue("A").originate(node("C"), "Cero", 0); net.run()
        assertEquals(start, net.established.size)
        net.nodes.getValue("A").originate(node("C"), "Un salto", 1); net.run()
        assertEquals(start + 1, net.established.size)
        assertTrue(net.received("C").isEmpty())
        assertTrue(net.nodes.getValue("B").snapshot().events.any { it.detail.contains("TTL agotado") })
    }

    @Test fun corruptTransferNeverReachesReceiverEngineOrCreatesSuccessfulLinkState() {
        val net = chain(); net.corruptFrom = "A"
        net.nodes.getValue("A").originate(node("C"), "Corrupción".repeat(20), 2); net.run()
        assertTrue(net.received("B").isEmpty()); assertTrue(net.received("C").isEmpty())
        assertTrue(net.written("A").isEmpty()); assertTrue(net.failure("A"))
        assertEquals(0, net.nodes.getValue("B").reassemblySnapshot().bufferedBytes)
    }

    @Test fun incompleteTransferWithFalsePlatformSuccessCannotAdvanceAndTimesOut() {
        val net = chain(); net.dropFrom = "A"
        net.nodes.getValue("A").originate(node("C"), "Incompleto".repeat(10), 2); net.run()
        assertTrue(net.nodes.getValue("A").client.busy)
        assertTrue(net.written("A").isEmpty()); assertTrue(net.received("C").isEmpty())
        net.clock.time = 5_000
        net.nodes.getValue("A").tick(); net.run()
        assertTrue(net.failure("A")); assertFalse(net.nodes.getValue("A").client.busy)
        assertEquals(0, net.nodes.getValue("B").reassemblySnapshot().bufferedBytes)
    }

    @Test fun queueHasEightSlotsIncludingActiveAndHandoffDoesNotMeanDelivery() {
        val net = chain()
        val a = net.nodes.getValue("A")
        a.setDirectBusy(true)
        repeat(9) { a.originate(node("B"), "Mensaje $it", 1) }
        assertEquals(8, a.snapshot().queued)
        assertEquals(8, a.snapshot().events.count { it.kind == MeshActivityKind.QUEUED })
        assertTrue(net.failure("A")); assertTrue(net.written("A").isEmpty()); assertTrue(net.received("B").isEmpty())
        a.setDirectBusy(false); net.run()
        assertEquals(8, net.received("B").size)
        assertEquals(0, a.snapshot().queued)
    }

    @Test fun queuedJobsExpireWhileLegacyChatOwnsTheLink() {
        val net = chain(); val a = net.nodes.getValue("A")
        a.setDirectBusy(true); a.originate(node("B"), "Cola esperando", 1)
        net.clock.time = 30_000; a.tick(); a.setDirectBusy(false); net.run()
        assertEquals(0, a.snapshot().queued)
        assertTrue(a.snapshot().events.any { it.detail.contains("Venció espera") })
        assertTrue(net.written("A").isEmpty())
    }

    @Test fun reconnectUsesFreshSessionAndTransferIdButKeepsLogicalIdentity() {
        val net = chain(); val a = net.nodes.getValue("A")
        a.originate(node("B"), "Primero", 1); net.run()
        a.originate(node("B"), "Segundo", 1); net.run()
        val frames = net.commands.filter { it.first == "A" }.map { it.second }.filterIsInstance<MeshGattCommand.WriteFrame>()
        val ids = frames.map { MeshFragmentProtocol.parse(it.value).transferId }.distinct()
        assertEquals(2, ids.size)
        assertEquals(net.sessions.size, net.sessions.distinct().size)
        assertEquals(listOf(node("A"), node("A")), net.received("B").map { it.originId })
        assertTrue(net.connections.isEmpty())
    }

    @Test fun rediscoveryAfterDisconnectRestoresMultihopWithoutChangingNodeIdentity() {
        val net = chain()
        val a = net.nodes.getValue("A")
        net.nodes.getValue("B").setEnabled(false)
        a.originate(node("C"), "Antes de recuperar B", 2); net.run()
        assertTrue(net.failure("A")); assertTrue(net.received("C").isEmpty())
        net.nodes.getValue("B").setEnabled(true)
        net.clock.time = 30_001 // Bounded probe cooldown, no automatic message retry.
        net.discover()
        a.originate(node("C"), "Después de recuperar B", 2); net.run()
        assertEquals(listOf("Después de recuperar B"), net.received("C").map { it.content })
        assertEquals(node("A"), net.received("C").single().originId)
    }

    @Test fun fanoutLargerThanAvailableQueueIsRejectedAtomically() {
        val net = FakeGattNetwork()
        val targets = (1..9).map { "N$it" }
        net.add("A", *targets.toTypedArray())
        targets.forEach { net.connect("A", it) }; net.discover()
        assertEquals(9, net.nodes.getValue("A").snapshot().neighbors.count { it.nodeId != null })
        val start = net.established.size
        net.nodes.getValue("A").originate(null, "Fanout sin capacidad", 3); net.run()
        assertEquals(start, net.established.size)
        assertTrue(targets.all { net.received(it).isEmpty() })
        assertTrue(net.failure("A"))
        assertEquals(0, net.nodes.getValue("A").snapshot().queued)
    }

    @Test fun cancellingParticipationDropsQueueAndLateCallbacksCannotResumeIt() {
        val net = chain(); val a = net.nodes.getValue("A")
        a.originate(node("B"), "Cancelado", 1)
        val token = a.client.job!!.token
        a.setEnabled(false)
        // Directly inject late callbacks; fake I/O is not allowed to create a new operation from them.
        a.client.connected(token, true); a.client.services(token, true); a.client.frameWritten(token, true)
        assertFalse(a.client.busy)
        assertEquals(0, a.snapshot().queued)
        assertTrue(net.written("A").isEmpty())
    }

    @Test fun serverRequiresValidIdentityAndRefusesRebindingWithinOneSession() {
        val clock = FakeClock(); val closed = mutableListOf<MeshLinkSession>()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true }, closeIncomingLink = closed::add)
        receiver.setEnabled(true)
        val session = session()
        assertTrue(receiver.openIncoming(session))
        val frames = MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 1)
        assertFalse(receiver.acceptFrame(session, frames[0]))
        assertFalse(receiver.acceptIdentity(session, byteArrayOf()))
        assertTrue(receiver.acceptIdentity(session, MeshLinkProtocol.hello(node("A"))))
        assertTrue(receiver.acceptIdentity(session, MeshLinkProtocol.hello(node("A"))))
        assertFalse(receiver.acceptIdentity(session, MeshLinkProtocol.hello(node("C"))))
        assertEquals(listOf(session), closed)
        assertFalse(receiver.acceptFrame(session, frames[0]))
        assertEquals(0, receiver.snapshot().incoming)
    }

    @Test fun incomingSessionsAreBoundedAndIdleConnectionsReleasePartialBuffers() {
        val clock = FakeClock(); val closed = mutableListOf<MeshLinkSession>()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true }, closeIncomingLink = closed::add)
        receiver.setEnabled(true)
        repeat(8) { assertTrue(receiver.openIncoming(session(it.toLong()))) }
        assertFalse(receiver.openIncoming(session(9)))
        receiver.acceptIdentity(session(0), MeshLinkProtocol.hello(node("A")))
        receiver.acceptFrame(session(0), MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 1)[0])
        assertTrue(receiver.reassemblySnapshot().bufferedBytes > 0)
        clock.time = 20_000; receiver.tick()
        assertEquals(8, closed.size)
        assertEquals(0, receiver.snapshot().incoming)
        assertEquals(0, receiver.reassemblySnapshot().bufferedBytes)
    }

    @Test fun legacyNeighborRemainsUsableAsLegacyButNeverBecomesMeshCapable() {
        val net = FakeGattNetwork(); net.add("A"); net.legacy.add("LEGACY"); net.connect("A", "LEGACY"); net.discover()
        assertNull(net.nodes.getValue("A").snapshot().neighbors.single().nodeId)
        val packet = DirectBleProtocol.encode("ALFA", "sí:🆘")
        assertEquals("sí:🆘", DirectBleProtocol.decode(packet).message)
        net.nodes.getValue("A").originate(null, "No mesh neighbor", 3); net.run()
        assertTrue(net.written("A").isEmpty()); assertTrue(net.failure("A"))
        assertTrue(net.commands.none { it.second is MeshGattCommand.WriteFrame })
    }

    @Test fun discoveredHandlesHistoryAndQueueStayBoundedUnderFlood() {
        val receiver = MeshBleNode(node("A"), FakeClock(), MeshGattPort { true })
        receiver.setEnabled(true)
        repeat(100) { receiver.discovered("radio-$it"); receiver.originate(null, "Mensaje $it", 1) }
        assertEquals(16, receiver.snapshot().neighbors.size)
        assertEquals(32, receiver.snapshot().events.size)
        assertTrue(receiver.snapshot().queued <= 8)
    }

    @Test fun invalidEnvelopeWithValidCrcNeverBecomesLocalReceipt() {
        val receiver = MeshBleNode(node("B"), FakeClock(), MeshGattPort { true })
        receiver.setEnabled(true); receiver.openIncoming(session())
        receiver.acceptIdentity(session(), MeshLinkProtocol.hello(node("A")))
        val bad = MeshMessageCodec.encode(message()).also { it[0] = 2 }
        val frames = MeshFragmentProtocol.fragment(bad, 1)
        frames.dropLast(1).forEach { assertTrue(receiver.acceptFrame(session(), it)) }
        assertFalse(receiver.acceptFrame(session(), frames.last()))
        assertTrue(receiver.snapshot().events.none { it.kind == MeshActivityKind.LOCAL_RECEIVED })
        assertFalse(receiver.originate(null, "🆘".repeat(257), 3))
        assertFalse(receiver.originate(null, "TTL inválido", 17))
    }
}
