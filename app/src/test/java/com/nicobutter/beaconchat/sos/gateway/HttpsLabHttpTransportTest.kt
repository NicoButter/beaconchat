package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.sos.incident
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class HttpsLabHttpTransportTest {
    private class Connection : HttpsURLConnection(URL(labEndpoint.url)) {
        val sent = ByteArrayOutputStream()
        var body = response(incident().incidentId).body.toByteArray(Charsets.UTF_8)
        var status = 201
        var disconnected = false
        var reads = 0
        var failRead = false
        var beforeOutput: () -> Unit = {}
        var disconnects = 0
        val declaredLength get() = fixedContentLength
        override fun connect() { }
        override fun disconnect() { disconnected = true; disconnects++ }
        override fun usingProxy() = false
        override fun getCipherSuite() = "FAKE_FOR_TEST_ONLY"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
        override fun getOutputStream(): ByteArrayOutputStream { beforeOutput(); return sent }
        override fun getInputStream(): InputStream {
            reads++
            if (failRead) throw SocketTimeoutException()
            return ByteArrayInputStream(body)
        }
        override fun getResponseCode() = status
        override fun getContentType() = "application/json; charset=utf-8"
    }
    private val request = LabHttpRequest(labEndpoint, LabIncidentPayload.from(incident()))

    @Test fun defaultDisabledTransportDoesNotEvenOpenAConnection() {
        var opened = 0
        val transport = HttpsLabHttpTransport { opened++; Connection() }
        assertEquals(LabDeliveryResult.Error, LabApiClient(transport).deliver(request))
        assertEquals(0, opened)
    }
    @Test fun postsVersionedJsonWithStableIdempotencyHeaderAndLeavesTlsVerificationUntouched() {
        val connection = Connection()
        val verifier = connection.hostnameVerifier; val factory = connection.sslSocketFactory
        val transport = HttpsLabHttpTransport { connection }; transport.setEnabled(true)
        assertEquals(LabDeliveryResult.Confirmed(LabReceipt(incident().incidentId, "lab_fake_1")), LabApiClient(transport).deliver(request))
        assertEquals("POST", connection.requestMethod)
        assertEquals(request.idempotencyKey, connection.getRequestProperty("Idempotency-Key"))
        assertEquals("application/json; charset=utf-8", connection.getRequestProperty("Content-Type"))
        assertEquals(request.payload.json(), connection.sent.toString("UTF-8"))
        assertEquals(connection.sent.size(), connection.declaredLength)
        assertEquals(5000, connection.connectTimeout); assertEquals(5000, connection.readTimeout)
        assertSame(verifier, connection.hostnameVerifier); assertSame(factory, connection.sslSocketFactory)
        assertFalse(connection.instanceFollowRedirects); assertFalse(connection.useCaches)
        assertTrue(connection.disconnected)
    }
    @Test fun redirectsNeverFollowOrConsumeTheResponseBody() {
        val connection = Connection().also { it.status = 307 }
        var opened = 0
        val transport = HttpsLabHttpTransport { opened++; connection }; transport.setEnabled(true)
        assertEquals(LabDeliveryResult.Unknown, LabApiClient(transport).deliver(request))
        assertEquals(1, opened); assertEquals(0, connection.reads)
        assertFalse(connection.instanceFollowRedirects); assertTrue(connection.disconnected)
    }
    @Test fun oversizedOrMalformedUtf8ResponseCannotBecomeAConfirmation() {
        for (body in listOf(ByteArray(2049) { ' '.code.toByte() }, byteArrayOf(0xc3.toByte(), 0x28))) {
            val connection = Connection().also { it.body = body }
            val transport = HttpsLabHttpTransport { connection }; transport.setEnabled(true)
            assertEquals(LabDeliveryResult.Unknown, LabApiClient(transport).deliver(request))
            assertTrue(connection.disconnected)
        }
    }
    @Test fun readTimeoutIsExplicitAndConnectionAlwaysCloses() {
        val connection = Connection().also { it.failRead = true }
        val transport = HttpsLabHttpTransport { connection }; transport.setEnabled(true)
        assertEquals(LabDeliveryResult.Timeout, LabApiClient(transport).deliver(request))
        assertTrue(connection.disconnected)
    }
    @Test fun revokedActivationCannotBeReusedByADelayedWorkerAfterReenable() {
        var opened = 0
        val transport = HttpsLabHttpTransport { opened++; Connection() }
        transport.setEnabled(true)
        val oldWorker = transport.authorized()
        transport.setEnabled(false); transport.setEnabled(true)
        assertEquals(LabDeliveryResult.Error, LabApiClient(oldWorker).deliver(request))
        assertEquals(0, opened)
        assertTrue(LabApiClient(transport.authorized()).deliver(request) is LabDeliveryResult.Confirmed)
        assertEquals(1, opened)
    }
    @Test fun revocationDoesNoIoAndDelayedCancellationOnlyClosesCapturedConnection() {
        val first = Connection(); val second = Connection()
        val connections = listOf(first, second).iterator()
        val transport = HttpsLabHttpTransport { connections.next() }
        var cancel: () -> Unit = {}
        first.beforeOutput = {
            transport.setEnabled(false)
            assertFalse(first.disconnected) // Safe for a main-looper revocation.
            cancel = transport.cancellation()
        }
        transport.setEnabled(true)
        assertEquals(LabDeliveryResult.Error, LabApiClient(transport).deliver(request))
        second.beforeOutput = { cancel(); assertFalse(second.disconnected) }
        transport.setEnabled(true)
        assertTrue(LabApiClient(transport).deliver(request) is LabDeliveryResult.Confirmed)
        assertEquals(2, first.disconnects); assertEquals(1, second.disconnects)
    }
}
