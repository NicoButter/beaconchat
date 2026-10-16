package com.nicobutter.beaconchat.mesh.core

import org.junit.Assert.*
import org.junit.Test

class MeshTopologyTest {
    @Test fun twoNodesExchangeIntactTextInBothDirections() {
        val net = SimulatedNetwork()
        net.add("A"); net.add("B"); net.connect("A", "B")
        net.publish("A", "B", "Mensaje íntegro: sí 🆘", 1)
        net.publish("B", "A", "Respuesta sin radio", 1)
        net.runUntilIdle()
        assertEquals("Mensaje íntegro: sí 🆘", net.deliveries.getValue(node("B")).single().content)
        assertEquals("Respuesta sin radio", net.deliveries.getValue(node("A")).single().content)
        assertEquals(2, net.frames.size)
    }

    @Test fun chainRequiresBAndPreservesTheOriginalEnvelope() {
        val net = SimulatedNetwork()
        net.add("A"); net.add("B"); net.add("C")
        net.connect("A", "B"); net.connect("B", "C")
        val original = net.publish("A", "C", "Ayuda: señal íntegra 🆘", 2).message
        net.runUntilIdle()
        assertFalse(node("C") in net.links.getValue(node("A")))
        assertEquals(original.copy(hopsRemaining = 0), net.deliveries.getValue(node("C")).single())
        assertTrue(net.deliveries.getValue(node("B")).isEmpty())
        assertEquals(1, net.attempts.count { it.from == node("B") })
        assertEquals(listOf(node("A"), node("B")), net.frames.map { it.from })
    }

    @Test fun removingTheIntermediateLinkMakesDestinationUnreachable() {
        val net = SimulatedNetwork()
        net.add("A"); net.add("B"); net.add("C")
        net.connect("A", "B")
        net.publish("A", "C", "Sin enlace B-C", 3)
        net.runUntilIdle()
        assertTrue(net.deliveries.getValue(node("C")).isEmpty())
        assertEquals(DispatchStatus.UNAVAILABLE, net.attempts.last().outcome)
        assertTrue(net.devices.values.all { it.engine.snapshot().pendingMessages == 0 })
    }

    @Test fun triangleHasNoLoopsOrDuplicateDelivery() {
        val net = SimulatedNetwork()
        listOf("A", "B", "C").forEach { net.add(it) }
        net.connect("A", "B"); net.connect("B", "C"); net.connect("C", "A")
        net.publish("A", "C", "Triángulo", 16)
        net.runUntilIdle(maxCycles = 8, maxFrames = 6)
        assertEquals(1, net.deliveries.getValue(node("C")).size)
        assertEquals(1, net.attempts.count { it.from == node("B") })
        val count = net.frames.size
        net.runUntilIdle()
        assertEquals(count, net.frames.size)
    }

    @Test fun broadcastInSixNodeRingDeliversOncePerNodeAndTerminates() {
        val net = SimulatedNetwork()
        val names = listOf("A", "B", "C", "D", "E", "F")
        names.forEach { net.add(it) }
        names.indices.forEach { net.connect(names[it], names[(it + 1) % names.size]) }
        net.publish("A", null, "Broadcast", 16)
        net.runUntilIdle(maxCycles = 12, maxFrames = 12)
        names.forEach { assertEquals(it, 1, net.deliveries.getValue(node(it)).size) }
        assertTrue(net.attempts.groupBy { it.from }.values.all { it.size == 1 })
        assertTrue(net.frames.all { it.message.hopsRemaining < 16 })
    }

    @Test fun zeroTtlNeverLeavesSourceAndExhaustedTtlNeverLeavesB() {
        val net = SimulatedNetwork()
        listOf("A", "B", "C").forEach { net.add(it) }
        net.connect("A", "B"); net.connect("B", "C")
        assertEquals(Forwarding.TTL_EXHAUSTED, net.publish("A", "C", "Cero", 0).forwarding)
        net.runUntilIdle()
        assertTrue(net.frames.isEmpty())
        net.publish("A", "C", "Un salto", 1)
        net.runUntilIdle()
        assertEquals(1, net.frames.size)
        assertEquals(0, net.frames.single().message.hopsRemaining)
        assertTrue(net.deliveries.getValue(node("C")).isEmpty())
        assertTrue(net.attempts.none { it.from == node("B") })
    }

    @Test fun broadcastWithZeroTtlCanDeliverOnlyLocally() {
        val net = SimulatedNetwork()
        net.add("A"); net.add("B"); net.connect("A", "B")
        val result = net.publish("A", null, "Local", 0)
        net.runUntilIdle()
        assertTrue(result.deliverLocally)
        assertEquals(1, net.deliveries.getValue(node("A")).size)
        assertTrue(net.deliveries.getValue(node("B")).isEmpty())
        assertTrue(net.frames.isEmpty())
    }

    @Test fun sameCallsignSameMessageIdAndChangedMacDoNotMergeLogicalOrigins() {
        val net = SimulatedNetwork()
        net.add("A", callsign = "SAME"); net.add("B"); net.add("C", callsign = "SAME")
        net.connect("A", "B"); net.connect("C", "B")
        val sharedId = id("shared")
        net.publish("A", "B", "Desde A", 1, sharedId)
        net.publish("C", "B", "Desde C", 1, sharedId)
        net.runUntilIdle()
        net.devices.getValue(node("A")).mac = "changed-MAC"
        net.publish("A", "B", "Desde A", 1, sharedId) // Same logical retry, different MAC.
        net.publish("A", "B", "Nuevo desde A", 1, id("new"))
        net.runUntilIdle()
        val received = net.deliveries.getValue(node("B"))
        assertEquals(listOf("Desde A", "Desde C", "Nuevo desde A"), received.map { it.content })
        assertNotEquals(received[0].originId, received[1].originId)
        assertEquals(received[0].originId, received[2].originId)
        assertEquals("changed-MAC", net.frames.last().sourceMac)
    }

    @Test fun duplicatePathsDeliverAndForwardAtMostOncePerKey() {
        val net = SimulatedNetwork()
        listOf("A", "B", "C", "D", "E").forEach { net.add(it) }
        net.connect("A", "B"); net.connect("A", "C")
        net.connect("B", "D"); net.connect("C", "D"); net.connect("D", "E")
        val first = net.publish("A", "E", "Dos caminos", 4)
        assertEquals(Acceptance.DUPLICATE,
            net.publish("A", "E", "Dos caminos", 4, first.message.messageId).acceptance)
        net.runUntilIdle(maxFrames = 20)
        assertEquals(1, net.attempts.count { it.from == node("D") })
        assertEquals(1, net.deliveries.getValue(node("E")).size)
    }
}
