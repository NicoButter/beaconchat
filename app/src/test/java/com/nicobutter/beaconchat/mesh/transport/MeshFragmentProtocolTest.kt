package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MeshFragmentProtocolTest {
    @Test fun goldenFramesFixMetadataAndStandardCrc32Vector() {
        val frames = MeshFragmentProtocol.fragment("123456789".toByteArray(), 0x1234)
        assertEquals(2, frames.size)
        assertArrayEquals(hex("f901123400020009cbf439263132333435363738"), frames[0])
        assertArrayEquals(hex("f901123401020009cbf4392639"), frames[1])
    }

    @Test fun everyPermittedLengthHasCanonicalFramesAtMost20Bytes() {
        for (size in 1..MeshFragmentProtocol.MAX_TRANSFER_BYTES) {
            val payload = ByteArray(size) { it.toByte() }
            val frames = MeshFragmentProtocol.fragment(payload, 65535)
            assertEquals((size + 7) / 8, frames.size)
            frames.forEachIndexed { index, frame ->
                assertTrue(frame.size in 13..20)
                val header = MeshFragmentProtocol.parse(frame)
                assertEquals(65535, header.transferId)
                assertEquals(index, header.index)
                assertEquals(size, header.totalBytes)
                assertEquals(frames.size, header.count)
                if (index != frames.lastIndex) assertEquals(20, frame.size)
            }
            assertArrayEquals(payload, frames.flatMap { it.drop(12) }.toByteArray())
        }
    }

    @Test fun maximumUnicastAndBroadcastNeed135And133Frames() {
        assertEquals(135, MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message("a".repeat(1024))), 0).size)
        assertEquals(133, MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message("a".repeat(1024), false)), 0).size)
    }

    @Test fun rejectsInvalidPayloadSizesAndTransferIds() {
        rejects { MeshFragmentProtocol.fragment(byteArrayOf(), 0) }
        rejects { MeshFragmentProtocol.fragment(ByteArray(1078), 0) }
        rejects { MeshFragmentProtocol.fragment(byteArrayOf(1), -1) }
        rejects { MeshFragmentProtocol.fragment(byteArrayOf(1), 65536) }
    }

    @Test fun rejectsTruncationOversizeAndShortIntermediateFrames() {
        val frames = MeshFragmentProtocol.fragment(ByteArray(17), 1)
        for (length in frames[0].indices) rejects { MeshFragmentProtocol.parse(frames[0].copyOf(length)) }
        rejects { MeshFragmentProtocol.parse(frames[0] + 0) }
        rejects { MeshFragmentProtocol.parse(frames.last() + 0) }
    }

    @Test fun rejectsUnknownVersionsAndWrongMarker() {
        val frame = MeshFragmentProtocol.fragment(ByteArray(8), 1).single()
        listOf(0, 2, 255).forEach { version ->
            rejects { MeshFragmentProtocol.parse(frame.copyOf().also { it[1] = version.toByte() }) }
        }
        rejects { MeshFragmentProtocol.parse(frame.copyOf().also { it[0] = 1 }) }
    }

    @Test fun rejectsImpossiblePositionsCountsAndLengths() {
        val frame = MeshFragmentProtocol.fragment(ByteArray(9), 1)[0]
        listOf(2, 128, 255).forEach { index ->
            rejects { MeshFragmentProtocol.parse(frame.copyOf().also { it[4] = index.toByte() }) }
        }
        listOf(0, 1, 3, 136, 255).forEach { count ->
            rejects { MeshFragmentProtocol.parse(frame.copyOf().also { it[5] = count.toByte() }) }
        }
        listOf(0, 8, 17, 1078, 65535).forEach { size ->
            rejects { MeshFragmentProtocol.parse(frame.copyOf().also { it[6] = (size ushr 8).toByte(); it[7] = size.toByte() }) }
        }
    }

    @Test fun legacyPacketsAreNeverMeshAndMeshIsNeverLegacyUtf8() {
        val legacy = listOf(DirectBleProtocol.encode("A", "sí:🆘"),
            DirectBleProtocol.encode("ABCDEFGH", "12345678901"))
        legacy.forEach {
            rejects { MeshFragmentProtocol.parse(it) }
            assertNotNull(DirectBleProtocol.decode(it))
        }
        MeshFragmentProtocol.fragment(MeshMessageCodec.encode(message()), 42).forEach { frame ->
            try { DirectBleProtocol.decode(frame); fail("Mesh interpreted as legacy") }
            catch (_: IllegalArgumentException) { } catch (_: java.nio.charset.CharacterCodingException) { }
        }
    }

    @Test fun futureGattCharacteristicIsDistinctFromAllCurrentBleUuids() {
        listOf("0000BEEF", "0000C4A7", "00004D51").forEach {
            assertNotEquals(UUID.fromString("$it-0000-1000-8000-00805f9b34fb"), MeshGattContract.CHARACTERISTIC_UUID)
        }
    }
}
