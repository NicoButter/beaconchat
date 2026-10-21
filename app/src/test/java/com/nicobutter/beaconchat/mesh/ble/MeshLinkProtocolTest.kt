package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import com.nicobutter.beaconchat.mesh.core.node
import com.nicobutter.beaconchat.mesh.transport.*
import org.junit.Assert.*
import org.junit.Test

class MeshLinkProtocolTest {
    @Test fun identityAndCapabilitiesFit20BytesAndPreserveLogicalUuid() {
        val identity = node("logical, not MAC or callsign")
        val bytes = MeshLinkProtocol.hello(identity)
        assertEquals(20, bytes.size)
        assertArrayEquals(hex("fa010101"), bytes.copyOf(4))
        assertEquals(identity, MeshLinkProtocol.identity(bytes))
    }
    @Test fun rejectsTruncatedExtendedUnknownAndZeroIdentity() {
        val hello = MeshLinkProtocol.hello(node("A"))
        hello.indices.forEach { size -> rejects { MeshLinkProtocol.identity(hello.copyOf(size)) } }
        rejects { MeshLinkProtocol.identity(hello + 0) }
        for (index in 0..3) for (value in listOf(0, 2, 255)) {
            rejects { MeshLinkProtocol.identity(hello.copyOf().also { it[index] = value.toByte() }) }
        }
        rejects { MeshLinkProtocol.identity(hello.copyOf().also { it.fill(0, 4) }) }
    }
    @Test fun receiptsBindTransferPositionAndCrcWithoutPromisingMessageDelivery() {
        val frames = MeshFragmentProtocol.fragment(ByteArray(30) { it.toByte() }, 65535)
        val receipts = frames.map(MeshLinkProtocol::receipt)
        assertTrue(receipts.all { it.size == 12 })
        assertFalse(receipts[0].contentEquals(receipts[1]))
        assertFalse(receipts[0].contentEquals(MeshLinkProtocol.receipt(MeshFragmentProtocol.fragment(ByteArray(30), 65535)[0])))
        rejects { MeshLinkProtocol.receipt(byteArrayOf()) }
    }
    @Test fun transferIdsDoNotRepeatAcrossConnectionsOrWrapInsideRetention() {
        val ids = MeshTransferIds()
        repeat(65536) { assertEquals(it, ids.allocate(1)) }
        try { ids.allocate(MeshBleTiming.REASSEMBLY_MILLIS); fail("ID reused early") } catch (_: IllegalStateException) { }
        assertEquals(0, ids.allocate(MeshBleTiming.REASSEMBLY_MILLIS + 1))
        assertEquals(1, ids.allocate(MeshBleTiming.REASSEMBLY_MILLIS + 1))
    }
    @Test fun legacyPacketsCannotBecomeHelloOrMeshDataAndLegacyRulesStayIntact() {
        val legacy = DirectBleProtocol.encode("ABCDEFGH", "12345678901")
        assertEquals(20, legacy.size)
        assertEquals("12345678901", DirectBleProtocol.decode(legacy).message)
        rejects { MeshLinkProtocol.identity(legacy) }
        rejects { MeshLinkProtocol.receipt(legacy) }
        rejects { DirectBleProtocol.encode("ABCDEFGH", "123456789012") }
        val hello = MeshLinkProtocol.hello(node("A"))
        try { DirectBleProtocol.decode(hello); fail("Hello read as legacy") }
        catch (_: IllegalArgumentException) { } catch (_: java.nio.charset.CharacterCodingException) { }
        assertNotEquals(MeshGattContract.IDENTITY_UUID, MeshGattContract.CHARACTERISTIC_UUID)
    }
}
