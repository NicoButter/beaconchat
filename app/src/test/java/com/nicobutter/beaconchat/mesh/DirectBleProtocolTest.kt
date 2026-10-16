package com.nicobutter.beaconchat.mesh

import org.junit.Assert.*
import org.junit.Test

class DirectBleProtocolTest {
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Se aceptó un paquete inválido") } catch (_: IllegalArgumentException) {
        } catch (_: java.nio.charset.CharacterCodingException) { }
    }

    @Test fun advertisementCallsignRoundTrip() {
        val bytes = DirectBleProtocol.encodeCallsign("ALFA")
        assertEquals("ALFA", DirectBleProtocol.decodeCallsign(bytes))
        assertEquals("UNKNOWN", DirectBleProtocol.decodeCallsign(DirectBleProtocol.encodeCallsign("")))
    }
    @Test fun rejectsInvalidAdvertisementData() {
        rejects { DirectBleProtocol.decodeCallsign(byteArrayOf()) }
        rejects { DirectBleProtocol.decodeCallsign("123456789".toByteArray()) }
        rejects { DirectBleProtocol.decodeCallsign(byteArrayOf(0xC3.toByte())) }
        rejects { DirectBleProtocol.decodeCallsign("A:B".toByteArray()) }
        rejects { DirectBleProtocol.decodeCallsign(" ".toByteArray()) }
    }
    @Test fun roundTripPreservesUnicodeAndColons() {
        val packet = DirectBleProtocol.encode("A1", "sí:🆘")
        assertEquals(DirectBleProtocol.Packet("A1", "sí:🆘"), DirectBleProtocol.decode(packet))
    }
    @Test fun exactMandatoryMtuBoundary() {
        val bytes = DirectBleProtocol.encode("ABCDEFGH", "12345678901")
        assertEquals(20, bytes.size)
        assertEquals("12345678901", DirectBleProtocol.decode(bytes).message)
    }
    @Test fun rejectsOversizeWithoutTruncation() {
        rejects { DirectBleProtocol.encode("ABCDEFGH", "123456789012") }
    }
    @Test fun countsUtf8BytesInsteadOfCharacters() {
        assertEquals(11, DirectBleProtocol.messageLimit("ABCDEFGH"))
        DirectBleProtocol.encode("ABCDEFGH", "áááááa")
        rejects { DirectBleProtocol.encode("ABCDEFGH", "áááááá") }
    }
    @Test fun emojiBoundary() {
        DirectBleProtocol.encode("A", "🆘🆘🆘🆘ab")
        rejects { DirectBleProtocol.encode("A", "🆘🆘🆘🆘abc") }
    }
    @Test fun validatesCallsignBytes() {
        assertEquals("UNKNOWN", DirectBleProtocol.callsign(""))
        assertEquals(12, DirectBleProtocol.messageLimit(""))
        rejects { DirectBleProtocol.encode("123456789", "a") }
        rejects { DirectBleProtocol.encode("ááááá", "a") }
        rejects { DirectBleProtocol.encode("A:B", "a") }
    }
    @Test fun rejectsBlankMessage() {
        rejects { DirectBleProtocol.encode("ABC", "") }
        rejects { DirectBleProtocol.encode("ABC", "  ") }
        rejects { DirectBleProtocol.decode("ABC: ".toByteArray()) }
    }
    @Test fun rejectsMalformedFrames() {
        listOf("", "ABC", ":help", "A:", "ABCDEFGHI:a", "A:" + "x".repeat(19)).forEach {
            rejects { DirectBleProtocol.decode(it.toByteArray()) }
        }
    }
    @Test fun rejectsMalformedUtf8InsteadOfReplacingIt() {
        rejects { DirectBleProtocol.decode(byteArrayOf(65, 58, 0xC3.toByte(), 0x28)) }
    }
    @Test fun rejectsBrokenSurrogatesInsteadOfReplacingThem() {
        rejects { DirectBleProtocol.encode("ABC", "\uD800") }
    }
    @Test fun preservesWhitespaceAndAllMessageBytes() {
        val text = " a:b \n"
        assertEquals(text, DirectBleProtocol.decode(DirectBleProtocol.encode("A", text)).message)
    }
}
