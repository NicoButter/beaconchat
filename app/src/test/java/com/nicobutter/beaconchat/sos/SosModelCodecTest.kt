package com.nicobutter.beaconchat.sos

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

internal fun incident(position: SosPosition? = null, note: String? = null, laboratory: Boolean = true) = SosIncident(
    UUID.fromString("00010203-0405-0607-0809-0a0b0c0d0e0f"), EmergencyType.MEDICAL, DeclaredSeverity.HIGH,
    1, note, position, laboratory)
internal fun rejectsSos(block: () -> Unit) {
    try { block(); fail("Entrada SOS inválida aceptada") } catch (_: IllegalArgumentException) { }
}
private fun raw(text: String) = Base64.getUrlDecoder().decode(text.removePrefix(SosCodec.PREFIX))
private fun wire(bytes: ByteArray) = SosCodec.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

class SosModelCodecTest {
    @Test fun missingPositionAndNoteAreValidAndRoundTripWithoutDefaults() {
        val original = incident()
        assertNull(original.position); assertNull(original.note)
        assertEquals(original, SosCodec.decode(SosCodec.encode(original)))
    }
    @Test fun fixedBinaryVectorLocksUuidTimeCodesFlagsAndLength() {
        val expected = "0104000102030405060708090a0b0c0d0e0f000000000000000101030000"
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertEquals(30, expected.size)
        assertArrayEquals(expected, raw(SosCodec.encode(incident())))
        assertEquals(incident(), SosCodec.decode(wire(expected)))
    }
    @Test fun realZeroCoordinatesAreValidButNeverUsedForUnknownPosition() {
        val zero = SosPosition(0.0, 0.0, null, PositionSource.GNSS, 0, PositionValidity.CURRENT)
        assertEquals(zero, SosCodec.decode(SosCodec.encode(incident(zero))).position)
        assertNull(incident().position)
        assertNull(SosMeshPolicy.laboratoryIncident(false, EmergencyType.OTHER, DeclaredSeverity.LOW, 0).position)
    }
    @Test fun acceptsCoordinateExtremesAndRejectsOutOfRangeAndNonFinite() {
        for (lat in listOf(-90.0, 90.0)) for (lon in listOf(-180.0, 180.0)) {
            val position = SosPosition(lat, lon, 0.0, PositionSource.GNSS, 0, PositionValidity.CURRENT)
            assertEquals(position, SosCodec.decode(SosCodec.encode(incident(position))).position)
        }
        listOf(-90.001, 90.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            rejectsSos { SosMeshPolicy.SYNTHETIC_POSITION.copy(latitude = it) }
        }
        listOf(-180.001, 180.001, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            rejectsSos { SosMeshPolicy.SYNTHETIC_POSITION.copy(longitude = it) }
        }
    }
    @Test fun accuracyIsOptionalEstimatedAndMustBeNonNegativeFinite() {
        listOf(null, 0.0, 0.1, 25.0, 10_000.0).forEach {
            val position = SosMeshPolicy.SYNTHETIC_POSITION.copy(accuracyMeters = it)
            assertEquals(position, SosCodec.decode(SosCodec.encode(incident(position))).position)
        }
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            rejectsSos { SosMeshPolicy.SYNTHETIC_POSITION.copy(accuracyMeters = it) }
        }
    }
    @Test fun ageAndValidityAreConsistentAtExactThresholds() {
        val base = SosMeshPolicy.SYNTHETIC_POSITION
        base.copy(ageMillis = 30_000)
        rejectsSos { base.copy(ageMillis = 30_001) }
        base.copy(ageMillis = 300_000, validity = PositionValidity.LAST_KNOWN)
        rejectsSos { base.copy(ageMillis = 300_001, validity = PositionValidity.LAST_KNOWN) }
        base.copy(ageMillis = 300_001, validity = PositionValidity.STALE)
        rejectsSos { base.copy(ageMillis = 300_000, validity = PositionValidity.STALE) }
        rejectsSos { base.copy(ageMillis = -1) }
        val old = base.copy(ageMillis = Long.MAX_VALUE, validity = PositionValidity.STALE)
        assertEquals(old, SosCodec.decode(SosCodec.encode(incident(old))).position)
    }
    @Test fun rejectsNullUuidInvalidTimeAndUnknownVersion() {
        rejectsSos { incident().copy(incidentId = UUID(0, 0)) }
        rejectsSos { incident().copy(createdAtEpochMillis = -1) }
        rejectsSos { incident().copy(version = 2) }
        val edge = incident().copy(createdAtEpochMillis = Long.MAX_VALUE)
        assertEquals(edge, SosCodec.decode(SosCodec.encode(edge)))
    }
    @Test fun noteUsesStrictUtf8ByteLimitsAndMaxRecordFits433Bytes() {
        listOf("a".repeat(256), "á".repeat(128), "🆘".repeat(64)).forEach { note ->
            val original = incident(SosMeshPolicy.SYNTHETIC_POSITION, note)
            val text = SosCodec.encode(original)
            assertEquals(320, raw(text).size)
            assertEquals(433, text.toByteArray(Charsets.UTF_8).size)
            assertTrue(text.toByteArray(Charsets.UTF_8).size <= 1024)
            assertEquals(original, SosCodec.decode(text))
        }
        listOf("a".repeat(257), "á".repeat(128) + "a", "🆘".repeat(65)).forEach {
            rejectsSos { incident(note = it) }
        }
    }
    @Test fun preservesWhitespaceUnicodeCompositionColonsAndNullCharacters() {
        val original = incident(note = " a:b\n sí 🆘 e\u0301 \u0000 ")
        assertEquals(original, SosCodec.decode(SosCodec.encode(original)))
    }
    @Test fun rejectsBlankNotesBrokenSurrogatesAndMalformedUtf8() {
        listOf("", " ", "\n\t", "\ud800", "a\udc00").forEach { rejectsSos { incident(note = it) } }
        val bytes = raw(SosCodec.encode(incident(note = "ab")))
        bytes[bytes.size - 2] = 0xc3.toByte(); bytes[bytes.size - 1] = 0x28
        rejectsSos { SosCodec.decode(wire(bytes)) }
        bytes[bytes.size - 2] = 32; bytes[bytes.size - 1] = 32
        rejectsSos { SosCodec.decode(wire(bytes)) }
    }
    @Test fun rejectsUnknownEnumIdsFlagsAndVersions() {
        val valid = raw(SosCodec.encode(incident(SosMeshPolicy.SYNTHETIC_POSITION)))
        for ((offset, values) in mapOf(0 to listOf(0, 2, 255), 1 to listOf(8, 255),
            26 to listOf(0, 5, 255), 27 to listOf(0, 5, 255), 28 to listOf(0, 4, 255), 29 to listOf(0, 4, 255))) {
            values.forEach { value -> rejectsSos { SosCodec.decode(wire(valid.copyOf().also { it[offset] = value.toByte() })) } }
        }
        val noPosition = raw(SosCodec.encode(incident())).also { it[1] = 2 }
        rejectsSos { SosCodec.decode(wire(noPosition)) }
    }
    @Test fun rejectsEveryTruncatedBinaryPrefixTrailingBytesAndFalseNoteLength() {
        for (original in listOf(incident(), incident(SosMeshPolicy.SYNTHETIC_POSITION, "sí 🆘"))) {
            val bytes = raw(SosCodec.encode(original))
            bytes.indices.forEach { length -> rejectsSos { SosCodec.decode(wire(bytes.copyOf(length))) } }
            rejectsSos { SosCodec.decode(wire(bytes + 0)) }
        }
        val bytes = raw(SosCodec.encode(incident(note = "a")))
        listOf(0, 2, 256, 257, 65535).forEach { length ->
            rejectsSos { SosCodec.decode(wire(bytes.copyOf().also { it[28] = (length ushr 8).toByte(); it[29] = length.toByte() })) }
        }
    }
    @Test fun rejectsNonCanonicalBase64UnknownPrefixesAndOversizedInputs() {
        val valid = SosCodec.encode(incident(note = "a"))
        listOf("", SosCodec.PREFIX, "OTHER:" + valid, valid + "=", valid + " ", valid + "+", valid + "/", "BCSOS:á", "BCSOS:" + "a".repeat(1024)).forEach {
            rejectsSos { SosCodec.decode(it) }
        }
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val changedPadBits = valid.dropLast(1) + alphabet[alphabet.indexOf(valid.last()) xor 1]
        rejectsSos { SosCodec.decode(changedPadBits) }
    }
    @Test fun allEnumsSourcesAndValidityRoundTripWithExplicitWireIds() {
        for (type in EmergencyType.values()) for (severity in DeclaredSeverity.values()) for (source in PositionSource.values()) {
            val original = incident(SosMeshPolicy.SYNTHETIC_POSITION.copy(source = source)).copy(type = type, severity = severity)
            assertEquals(original, SosCodec.decode(SosCodec.encode(original)))
        }
    }
    @Test fun canonicalBase64AgreesWithIndependentJdkEncoderForEveryNoteLength() {
        for (length in 0..256) {
            val original = incident(if (length % 2 == 0) SosMeshPolicy.SYNTHETIC_POSITION else null, "x".repeat(length).takeIf { it.isNotEmpty() })
            val text = SosCodec.encode(original)
            assertEquals(text, wire(raw(text)))
            assertEquals(original, SosCodec.decode(text))
        }
    }
    @Test fun malformedAndMutatedBytesAreBoundedAndNeverEscapeAsBufferExceptions() {
        val random = Random(5)
        val valid = raw(SosCodec.encode(incident(SosMeshPolicy.SYNTHETIC_POSITION, "sí 🆘")))
        repeat(3000) { index ->
            val bytes = if (index % 2 == 0) random.nextBytes(random.nextInt(0, 350))
                else valid.copyOf().also { it[random.nextInt(it.size)] = random.nextInt(256).toByte() }
            try {
                val decoded = SosCodec.decode(wire(bytes))
                assertEquals(decoded, SosCodec.decode(SosCodec.encode(decoded)))
            } catch (_: IllegalArgumentException) { }
        }
    }
}
