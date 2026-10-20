package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.sos.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.util.UUID
import javax.net.ssl.SSLException

/** A complete URL entered by the user. No default destination, URL credentials or redirects. */
class LabEndpoint private constructor(val url: String) {
    companion object {
        const val PATH = "/api/v1/lab/incidents"
        fun parse(value: String): LabEndpoint {
            require(value.length in 1..2048) { "Ingresá una URL HTTPS de laboratorio" }
            val uri = try { URI(value) } catch (_: Exception) {
                throw IllegalArgumentException("URL de laboratorio inválida")
            }
            require(uri.scheme == "https" && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
                uri.rawPath == PATH && (uri.port == -1 || uri.port in 1..65535)) {
                "Se exige HTTPS, sin credenciales ni query, con ruta $PATH"
            }
            return LabEndpoint(uri.toASCIIString())
        }
    }
}

/** Allowlist projection: notes, node IDs, callsigns and GNSS never enter an HTTP request. */
data class LabIncidentPayload private constructor(val incidentId: UUID, val type: EmergencyType,
    val severity: DeclaredSeverity, val createdAtEpochMillis: Long, val withSyntheticPosition: Boolean) {
    fun json(): String {
        val position = if (withSyntheticPosition) {
            val p = SosMeshPolicy.SYNTHETIC_POSITION
            ",\"position\":{\"latitude\":${p.latitude},\"longitude\":${p.longitude}}"
        } else ""
        return "{\"incidentId\":\"$incidentId\",\"type\":\"${type.name}\"," +
            "\"severity\":\"${severity.name}\",\"createdAtEpochMillis\":$createdAtEpochMillis$position}"
    }
    companion object {
        fun from(incident: SosIncident): LabIncidentPayload {
            require(SosMeshPolicy.permitted(incident)) { "Solo SOS sintéticos permitidos" }
            return LabIncidentPayload(incident.incidentId, incident.type, incident.severity,
                incident.createdAtEpochMillis, incident.position != null)
        }
    }
}

data class LabHttpRequest(val endpoint: LabEndpoint, val payload: LabIncidentPayload) {
    val idempotencyKey: String get() = payload.incidentId.toString()
}
data class LabHttpResponse(val status: Int, val contentType: String?, val body: String)
fun interface LabHttpTransport { fun post(request: LabHttpRequest): LabHttpResponse }
data class LabReceipt(val incidentId: UUID, val receipt: String)

sealed class LabDeliveryResult {
    data class Confirmed(val receipt: LabReceipt) : LabDeliveryResult()
    data class Rejected(val status: Int) : LabDeliveryResult()
    object Error : LabDeliveryResult()
    /** Both timeout and unknown may have occurred AFTER server persistence. Retry uses the same UUID. */
    object Timeout : LabDeliveryResult()
    object Unknown : LabDeliveryResult()
}

/** JVM client and strict V1 response validation, independent of Android and of the queue. */
class LabApiClient(private val transport: LabHttpTransport) {
    fun deliver(request: LabHttpRequest): LabDeliveryResult = try {
        val response = transport.post(request)
        when {
            response.status in 400..499 && response.status !in listOf(408, 429) ->
                LabDeliveryResult.Rejected(response.status)
            response.status == 200 || response.status == 201 -> {
                val receipt = LabApiContract.receipt(response, request.payload.incidentId)
                if (receipt == null) LabDeliveryResult.Unknown else LabDeliveryResult.Confirmed(receipt)
            }
            else -> LabDeliveryResult.Unknown
        }
    } catch (_: SocketTimeoutException) { LabDeliveryResult.Timeout
    } catch (_: SSLException) { LabDeliveryResult.Error
    } catch (_: IOException) { LabDeliveryResult.Unknown
    } catch (_: Exception) { LabDeliveryResult.Error }
}

object LabApiContract {
    const val MAX_RESPONSE_BYTES = 2048
    const val IO_TIMEOUT_MILLIS = 5000

    fun receipt(response: LabHttpResponse, expectedId: UUID): LabReceipt? = try {
        require(response.status == 200 || response.status == 201)
        val media = response.contentType?.lowercase()?.replace(" ", "")
        require(media == "application/json" || media == "application/json;charset=utf-8")
        require(response.body.length <= MAX_RESPONSE_BYTES && strictUtf8(response.body).size <= MAX_RESPONSE_BYTES)
        val fields = ReceiptJson(response.body).read()
        require(fields.keys == setOf("version", "incidentId", "receipt", "persistedInLaboratory"))
        require(fields["version"] == "1" && fields["incidentId"] == expectedId.toString())
        require(fields["persistedInLaboratory"] == true)
        val token = fields["receipt"] as? String ?: throw IllegalArgumentException()
        require(token.matches(Regex("lab_[A-Za-z0-9_-]{1,96}")))
        LabReceipt(expectedId, token)
    } catch (_: IllegalArgumentException) { null }
}

/** Deliberately narrow JSON contract: one flat object, string/boolean values, no duplicate keys.
 * Full JSON string escapes are accepted; nested values, unknown fields and trailing data are rejected.
 * This avoids Android's org.json stubs in JVM tests without adding a JSON dependency for four fields.
 */
private class ReceiptJson(private val text: String) {
    private var at = 0
    private fun space() { while (at < text.length && text[at] in " \t\r\n") at++ }
    private fun take(c: Char) { space(); require(at < text.length && text[at++] == c) }
    fun read(): Map<String, Any> {
        take('{')
        val result = linkedMapOf<String, Any>()
        space()
        if (at < text.length && text[at] == '}') { at++ } else {
            while (true) {
                require(result.size < 4)
                val key = string()
                require(key !in result)
                take(':'); space()
                val value: Any = when {
                    at < text.length && text[at] == '"' -> string()
                    text.startsWith("true", at) -> { at += 4; true }
                    text.startsWith("false", at) -> { at += 5; false }
                    else -> throw IllegalArgumentException()
                }
                result[key] = value
                space(); require(at < text.length)
                if (text[at] == '}') { at++; break }
                take(',')
            }
        }
        space(); require(at == text.length)
        return result
    }
    private fun string(): String {
        take('"')
        val out = StringBuilder()
        while (at < text.length) {
            val c = text[at++]
            if (c == '"') return out.toString()
            require(c >= ' ')
            if (c != '\\') { out.append(c); continue }
            require(at < text.length)
            out.append(when (val escape = text[at++]) {
                '"', '\\', '/' -> escape
                'b' -> '\b'
                'f' -> '\u000c'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    require(at + 4 <= text.length)
                    val hex = text.substring(at, at + 4)
                    require(hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
                    at += 4; hex.toInt(16).toChar()
                }
                else -> throw IllegalArgumentException()
            })
        }
        throw IllegalArgumentException()
    }
}
