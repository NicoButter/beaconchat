package com.nicobutter.beaconchat.mesh.transport

import com.nicobutter.beaconchat.mesh.core.MeshMessage
import com.nicobutter.beaconchat.mesh.core.MessageId
import com.nicobutter.beaconchat.mesh.core.NodeId
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

internal fun message(text: String = "Ayuda: sí 🆘", unicast: Boolean = true, ttl: Int = 3) = MeshMessage(
    MessageId(UUID.fromString("10111213-1415-1617-1819-1a1b1c1d1e1f")),
    NodeId(UUID.fromString("00010203-0405-0607-0809-0a0b0c0d0e0f")),
    if (unicast) NodeId(UUID.fromString("20212223-2425-2627-2829-2a2b2c2d2e2f")) else null,
    text, ttl
)

internal fun hex(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
internal fun session(number: Long = 1) = MeshLinkSession(UUID(0, number))
internal fun rejects(block: () -> Unit) {
    try { block(); fail("Invalid input accepted") } catch (_: IllegalArgumentException) { }
}

class MeshMessageCodecTest {
    @Test fun broadcastGoldenVectorFixesVersionFlagsUuidOrderTtlAndLength() {
        val bytes = hex("010003000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f000141")
        assertArrayEquals(bytes, MeshMessageCodec.encode(message("A", false)))
        assertEquals(message("A", false), MeshMessageCodec.decode(bytes))
        assertEquals(38, bytes.size)
    }

    @Test fun unicastGoldenVectorIncludesDestinationWithoutChangingOtherFields() {
        val bytes = hex("010103000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f" +
            "202122232425262728292a2b2c2d2e2f000141")
        assertArrayEquals(bytes, MeshMessageCodec.encode(message("A")))
        assertEquals(message("A"), MeshMessageCodec.decode(bytes))
        assertEquals(54, bytes.size)
    }

    @Test fun preservesUnicodeWhitespaceColonsNullAndNonNormalizedText() {
        val original = message(" \nAyuda: sí 🆘 e\u0301\u0000 \t")
        assertEquals(original, MeshMessageCodec.decode(MeshMessageCodec.encode(original)))
    }

    @Test fun roundTripsEverySupportedTtlWithoutDecrement() {
        for (ttl in 0..16) for (unicast in listOf(false, true)) {
            val original = message(unicast = unicast, ttl = ttl)
            assertEquals(original, MeshMessageCodec.decode(MeshMessageCodec.encode(original)))
        }
    }

    @Test fun exact1024AsciiByteBoundary() {
        for (unicast in listOf(false, true)) {
            val original = message("a".repeat(1024), unicast)
            val bytes = MeshMessageCodec.encode(original)
            assertEquals(if (unicast) 1077 else 1061, bytes.size)
            assertEquals(original, MeshMessageCodec.decode(bytes))
        }
        rejects { MeshMessageCodec.encode(message("a".repeat(1025))) }
    }

    @Test fun exact1024MultibyteBoundaryCountsBytesNotCharacters() {
        listOf("á".repeat(512), "🆘".repeat(256), "á".repeat(511) + "ab").forEach {
            assertEquals(1024, it.toByteArray(Charsets.UTF_8).size)
            assertEquals(message(it), MeshMessageCodec.decode(MeshMessageCodec.encode(message(it))))
        }
        listOf("á".repeat(512) + "a", "🆘".repeat(257)).forEach {
            rejects { MeshMessageCodec.encode(message(it)) }
        }
    }

    @Test fun rejectsEmptyAndBlankContentOnBothSides() {
        listOf("", " ", "\n\t", "\u2003").forEach { rejects { MeshMessageCodec.encode(message(it)) } }
        val blank = MeshMessageCodec.encode(message("a")).also { it[it.lastIndex] = 32 }
        rejects { MeshMessageCodec.decode(blank) }
        val empty = MeshMessageCodec.encode(message("a")).dropLast(1).toByteArray()
        empty[empty.lastIndex] = 0
        rejects { MeshMessageCodec.decode(empty) }
    }

    @Test fun rejectsUnpairedUtf16Surrogates() {
        listOf("\ud800", "\udc00", "a\ud800z").forEach { rejects { MeshMessageCodec.encode(message(it)) } }
    }

    @Test fun rejectsMalformedOverlongSurrogateAndTruncatedUtf8() {
        listOf(hex("c328"), hex("c080"), hex("eda080"), hex("f09f86"), hex("ff"), hex("80")).forEach { bad ->
            val bytes = MeshMessageCodec.encode(message("a".repeat(bad.size)))
            bad.copyInto(bytes, MeshMessageCodec.UNICAST_HEADER_BYTES)
            rejects { MeshMessageCodec.decode(bytes) }
        }
    }

    @Test fun rejectsUnknownMessageVersionsOnBothSides() {
        listOf(0, 2, 255).forEach { version ->
            rejects { MeshMessageCodec.encode(message().copy(version = version)) }
            rejects { MeshMessageCodec.decode(MeshMessageCodec.encode(message()).also { it[0] = version.toByte() }) }
        }
    }

    @Test fun rejectsUnknownFlagsAndInvalidTtl() {
        listOf(2, 3, 128, 255).forEach { flag ->
            rejects { MeshMessageCodec.decode(MeshMessageCodec.encode(message()).also { it[1] = flag.toByte() }) }
        }
        listOf(-1, 17, 255, 256).forEach { ttl -> rejects { MeshMessageCodec.encode(message(ttl = ttl)) } }
        listOf(17, 128, 255).forEach { ttl ->
            rejects { MeshMessageCodec.decode(MeshMessageCodec.encode(message()).also { it[2] = ttl.toByte() }) }
        }
    }

    @Test fun rejectsEveryTruncatedPrefixAndTrailingBytes() {
        for (unicast in listOf(false, true)) {
            val bytes = MeshMessageCodec.encode(message(unicast = unicast))
            for (length in bytes.indices) rejects { MeshMessageCodec.decode(bytes.copyOf(length)) }
            rejects { MeshMessageCodec.decode(bytes + 0) }
        }
        rejects { MeshMessageCodec.decode(ByteArray(MeshMessageCodec.MAX_ENCODED_BYTES + 1)) }
    }

    @Test fun rejectsFalseZeroOversizedAndUnsignedContentLengths() {
        for (length in listOf(0, 1, 3, 1024, 1025, 65535)) {
            val bytes = MeshMessageCodec.encode(message("ab"))
            bytes[51] = (length ushr 8).toByte()
            bytes[52] = length.toByte()
            rejects { MeshMessageCodec.decode(bytes) }
        }
    }

    @Test fun distinctLogicalOriginsWithSameMessageIdRemainDistinct() {
        val a = message()
        val b = a.copy(originId = NodeId(UUID(42, 99)))
        val decoded = listOf(a, b).map { MeshMessageCodec.decode(MeshMessageCodec.encode(it)) }
        assertEquals(a.messageId, decoded[1].messageId)
        assertNotEquals(decoded[0].key, decoded[1].key)
    }
}
