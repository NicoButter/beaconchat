package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import org.junit.Assert.*
import org.junit.Test

class MeshIncomingSessionsTest {
    @Test fun requestsRequireAnActualConnectionAndDuplicateConnectKeepsTheSession() {
        val links = MeshIncomingSessions()
        assertNull(links.getOrOpen("A") { true })
        assertTrue(links.connected("A"))
        assertNull(links.getOrOpen("A") { false })
        val first = links.getOrOpen("A") { true }!!
        assertTrue(links.connected("A"))
        assertEquals(first, links.getOrOpen("A") { fail("Opened twice"); false })
        assertEquals(first, links.disconnected("A"))
        assertNull(links.getOrOpen("A") { true })
        assertTrue(links.connected("A"))
        assertNotEquals(first, links.getOrOpen("A") { true })
    }

    @Test fun expiredPhysicalConnectionCannotMintAnotherSessionBeforeDisconnect() {
        val links = MeshIncomingSessions()
        val clock = FakeClock()
        val cancelled = mutableListOf<String>()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true },
            closeIncomingLink = { links.retire(it)?.let(cancelled::add) })
        receiver.setEnabled(true)
        links.connected("A")
        val first = links.getOrOpen("A", receiver::openIncoming)!!
        receiver.acceptIdentity(first, MeshLinkProtocol.hello(node("A")))
        receiver.acceptFrame(first, MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 1)[0])
        assertTrue(receiver.reassemblySnapshot().bufferedBytes > 0)
        clock.time = MeshBleTiming.INCOMING_IDLE_MILLIS
        // No ticker: the request itself enforces expiration.
        assertNull(receiver.readIdentity(first))
        assertEquals(listOf("A"), cancelled)
        assertEquals(0, receiver.reassemblySnapshot().bufferedBytes)
        repeat(10) {
            assertTrue(links.connected("A"))
            assertNull(links.getOrOpen("A", receiver::openIncoming))
        }
        assertEquals(0, receiver.snapshot().incoming)
        links.disconnected("A"); links.connected("A")
        val second = links.getOrOpen("A", receiver::openIncoming)!!
        assertNotEquals(first, second)
        assertTrue(receiver.acceptIdentity(second, MeshLinkProtocol.hello(node("A"))))
        assertFalse(receiver.acceptFrame(first, MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 2)[0]))
    }

    @Test fun closingConnectionsKeepTheirQuotaAndServerStopDropsAllIncarnations() {
        val links = MeshIncomingSessions(2)
        assertTrue(links.connected("A")); assertTrue(links.connected("B"))
        val a = links.getOrOpen("A") { true }!!
        val b = links.getOrOpen("B") { true }!!
        assertEquals("A", links.retire(a))
        assertFalse(links.connected("C"))
        assertEquals(listOf(b), links.clear())
        assertNull(links.getOrOpen("B") { true })
        assertTrue(links.connected("C"))
    }

    @Test fun rejectedTrafficCannotRefreshIncomingIdleDeadline() {
        val clock = FakeClock()
        val closed = mutableListOf<MeshLinkSession>()
        val receiver = MeshBleNode(node("B"), clock, MeshGattPort { true }, closeIncomingLink = closed::add)
        receiver.setEnabled(true); receiver.openIncoming(session())
        receiver.acceptIdentity(session(), MeshLinkProtocol.hello(node("A")))
        clock.time = MeshBleTiming.INCOMING_IDLE_MILLIS - 1
        assertFalse(receiver.acceptFrame(session(), byteArrayOf()))
        assertFalse(receiver.acceptIdentity(session(), byteArrayOf()))
        clock.time = MeshBleTiming.INCOMING_IDLE_MILLIS
        assertNull(receiver.readIdentity(session()))
        assertEquals(listOf(session()), closed)
    }
}
