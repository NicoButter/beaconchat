package com.nicobutter.beaconchat.sos.gateway

import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import javax.net.ssl.HttpsURLConnection

/** Android-compatible HTTPS I/O. Platform trust store and hostname verification are left intact.
 * One caller at a time, off main. Revocation is immediate; the host disconnects off main as well.
 * Connect/read have 5 s timeouts; the host also enforces a logical 20 s attempt deadline.
 */
class HttpsLabHttpTransport internal constructor(private val open: (URL) -> HttpsURLConnection) : LabHttpTransport {
    constructor() : this({ it.openConnection() as HttpsURLConnection })
    @Volatile private var current: HttpsURLConnection? = null
    @Volatile private var consent = false
    @Volatile private var generation = 0L

    fun setEnabled(value: Boolean) {
        generation++
        consent = value
    }

    /** Capture the old connection so a delayed cancellation cannot close a newer one. Run off main. */
    fun cancellation(): () -> Unit {
        val connection = current
        return { connection?.disconnect() }
    }

    /** Captures this activation, preventing a delayed worker from using a later activation's consent. */
    fun authorized(): LabHttpTransport {
        val token = generation
        return LabHttpTransport { post(it, token) }
    }

    override fun post(request: LabHttpRequest): LabHttpResponse = post(request, generation)

    private fun post(request: LabHttpRequest, token: Long): LabHttpResponse {
        check(consent) { "Salida HTTPS desactivada" }
        check(token == generation) { "Activación HTTPS vencida" }
        val target = LabEndpoint.parse(request.endpoint.url) // Defense against altered configuration.
        val connection = open(URL(target.url))
        current = connection
        try {
            check(consent && token == generation) { "Consentimiento revocado" }
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = LabApiContract.IO_TIMEOUT_MILLIS
            connection.readTimeout = LabApiContract.IO_TIMEOUT_MILLIS
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Idempotency-Key", request.idempotencyKey)
            val bytes = request.payload.json().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            check(consent && token == generation) { "Consentimiento revocado" }
            val status = connection.responseCode
            // Never follow Location or consume arbitrary error bodies.
            if (status != 200 && status != 201) return LabHttpResponse(status, null, "")
            val body = connection.inputStream.use { input ->
                val buffer = ByteArray(LabApiContract.MAX_RESPONSE_BYTES + 1)
                var count = 0
                while (count < buffer.size) {
                    check(consent && token == generation) { "Consentimiento revocado" }
                    val size = input.read(buffer, count, buffer.size - count)
                    if (size < 0) break
                    count += size
                }
                if (count > LabApiContract.MAX_RESPONSE_BYTES) throw IOException("Respuesta de laboratorio demasiado grande")
                Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(buffer, 0, count)).toString()
            }
            return LabHttpResponse(status, connection.contentType?.take(128), body)
        } finally {
            connection.disconnect()
            current = null
        }
    }
}
