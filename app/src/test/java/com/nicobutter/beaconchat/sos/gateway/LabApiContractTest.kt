package com.nicobutter.beaconchat.sos.gateway

import com.nicobutter.beaconchat.sos.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LabApiContractTest {
    private val id = incident().incidentId
    private val request = LabHttpRequest(labEndpoint, LabIncidentPayload.from(incident()))
    private fun deliver(response: LabHttpResponse) = LabApiClient(LabHttpTransport { response }).deliver(request)

    @Test fun acceptsOnly200Or201WithCorrelatedExplicitLaboratoryPersistence() {
        for (status in listOf(200, 201)) {
            assertEquals(LabDeliveryResult.Confirmed(LabReceipt(id, "lab_fake_1")), deliver(response(id, status)))
        }
        for (status in listOf(202, 204, 301, 307, 308, 408, 429, 500, 503)) {
            assertEquals(LabDeliveryResult.Unknown, deliver(response(id, status)))
        }
    }
    @Test fun successfulHttpWithoutAValidReceiptIsUnknown() {
        for (body in listOf("", "{}", "saved", "{\"ok\":true}",
            response(id).body.replace("\"lab_fake_1\"", "\"\""),
            response(id).body.replace("\"lab_fake_1\"", "\"rescued\""),
            response(id).body.replace(":true", ":false"),
            response(id).body.replace(":true", ":\"true\""),
            response(id).body.replace("\"version\":\"1\"", "\"version\":\"2\""))) {
            assertEquals(body, LabDeliveryResult.Unknown, deliver(response(id).copy(body = body)))
        }
    }
    @Test fun differentUuidAndNoncanonicalUuidNeverConfirm() {
        assertEquals(LabDeliveryResult.Unknown, deliver(response(UUID.randomUUID())))
        assertEquals(LabDeliveryResult.Unknown, deliver(response(id).copy(body = response(id).body.replace(id.toString(), id.toString().uppercase()))))
    }
    @Test fun rejectsDuplicateKeysTrailingJunkNestedValuesAndUnknownFields() {
        val valid = response(id).body
        for (body in listOf("$valid true", "$valid{}", valid.replace("{", "{\"version\":\"1\","),
            valid.replace("{", "{\"vers\\u0069on\":\"2\","),
            valid.replace("true", "{\"saved\":true}"), valid.replace("true", "[true]"),
            valid.replace("}", ",\"extra\":\"field\"}"), valid.replace("true", "null"),
            valid.replace("\"lab_fake_1\"", "\"lab_\\q\""))) {
            assertEquals(body, LabDeliveryResult.Unknown, deliver(response(id).copy(body = body)))
        }
    }
    @Test fun responseParserSupportsJsonWhitespaceOrderAndStringEscapes() {
        val body = " { \"persistedInLaboratory\" : true, \"receipt\":\"lab_\\u0031\",\n" +
            "\"incidentId\":\"$id\", \"version\":\"1\" }\r\n"
        assertEquals(LabDeliveryResult.Confirmed(LabReceipt(id, "lab_1")), deliver(response(id).copy(body = body)))
    }
    @Test fun enforcesJsonMediaTypeUtf8BodyLimitAndReceiptLimit() {
        for (type in listOf(null, "text/html", "application/jsonp", "application/json;charset=latin1")) {
            assertEquals(LabDeliveryResult.Unknown, deliver(response(id).copy(contentType = type)))
        }
        assertEquals(LabDeliveryResult.Unknown, deliver(response(id).copy(body = " ".repeat(2049))))
        assertEquals(LabDeliveryResult.Unknown, deliver(response(id, receipt = "lab_" + "a".repeat(97))))
        assertEquals(LabDeliveryResult.Unknown, deliver(response(id, receipt = "lab_\ud800")))
    }
    @Test fun requestContainsOnlyAllowlistedFieldsAndOptionalFixedPosition() {
        val sos = incident(SosMeshPolicy.SYNTHETIC_POSITION, "Nota privada NO ENVIAR -51.6")
        val json = LabIncidentPayload.from(sos).json()
        assertEquals("{\"incidentId\":\"$id\",\"type\":\"MEDICAL\",\"severity\":\"HIGH\"," +
            "\"createdAtEpochMillis\":1,\"position\":{\"latitude\":12.3456,\"longitude\":-45.6789}}", json)
        assertFalse(json.contains("Nota")); assertFalse(json.contains("-51.6"))
        val without = LabIncidentPayload.from(incident()).json()
        assertFalse(without.contains("position")); assertFalse(without.contains("0.0"))
    }
    @Test fun endpointRequiresHttpsExactVersionedPathAndNoCredentialsQueryOrFragment() {
        for (url in listOf("", "http://host${LabEndpoint.PATH}", "file:///tmp/incidents", "https://host/",
            "https://user:secret@host${LabEndpoint.PATH}", "https://host${LabEndpoint.PATH}?token=secret",
            "https://host${LabEndpoint.PATH}#fragment", "https://host:0${LabEndpoint.PATH}",
            "https://host:65536${LabEndpoint.PATH}", "https://host/api/v1/lab/%69ncidents",
            "https://host/api/v1/lab/../lab/incidents", "https://${"a".repeat(2048)}${LabEndpoint.PATH}")) {
            rejectsSos { LabEndpoint.parse(url) }
        }
        assertEquals("https://host:8443${LabEndpoint.PATH}", LabEndpoint.parse("https://host:8443${LabEndpoint.PATH}").url)
    }
}
