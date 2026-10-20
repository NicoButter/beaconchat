package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.sos.gateway.*

@Composable
fun SosInternetGatewayDialog(state: GatewaySnapshot, meshEnabled: Boolean,
    onEnable: (String) -> Unit, onDisable: () -> Unit, onDismiss: () -> Unit) {
    var endpoint by remember { mutableStateOf(state.endpointUrl ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Gateway HTTPS experimental") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Solo ensayos sintéticos. Los comprobantes de pruebas simuladas o de laboratorio no corresponden a un servicio de emergencia ni confirman rescate.")
                Text("La salida está desactivada por defecto. Configurá tu API de ensayo y activá explícitamente. No se envían notas ni posiciones GPS reales.")
                OutlinedTextField(value = endpoint, onValueChange = { if (it.length <= 2048) endpoint = it },
                    label = { Text("URL HTTPS de la API de ensayo") },
                    supportingText = { Text("Ruta requerida: ${LabEndpoint.PATH}. Sin credenciales en la URL.") },
                    enabled = !state.enabled, maxLines = 3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Habilitar gateway de laboratorio", Modifier.weight(1f))
                    Switch(checked = state.enabled, enabled = state.enabled || meshEnabled,
                        onCheckedChange = { enable ->
                            error = null
                            if (!enable) onDisable() else try { onEnable(endpoint.trim()) }
                            catch (e: IllegalArgumentException) { error = e.message }
                            catch (e: IllegalStateException) { error = e.message }
                        })
                }
                if (!meshEnabled) Text("Activá la participación mesh para recibir ensayos de otros nodos.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(if (!state.enabled) "Gateway desactivado" else if (state.connected)
                    "Internet validado por Android; la API todavía puede fallar" else "Sin Internet validado; no se inicia HTTPS")
                Text("Pendientes: ${state.queued}/16 · incidentes retenidos: ${state.tracked}/64")
                Text("Cola solo en memoria, vence a los 5 minutos. Hasta 3 intentos; intervalo mínimo de 5 s. Al pausar, salir o desactivar mesh se apaga el gateway y se descartan pendientes. Un envío iniciado puede haber sido guardado aunque falte respuesta.")
                Text("Los comprobantes quedan en este gateway; no vuelven al emisor por BLE.")
                state.lastAdmission?.let { Text("Última recepción: ${admissionLabel(it)}") }
                state.incidents.asReversed().forEach { incident ->
                    Divider()
                    SelectionContainer { Text("Incidente: ${incident.incidentId}") }
                    Text("${gatewayStateLabel(incident.state)} · intentos ${incident.attempts}/3")
                    if (incident.pending && incident.state != GatewayState.HTTP_STARTED) Text("Pendiente en cola")
                    incident.receipt?.let { SelectionContainer { Text("Comprobante de laboratorio: $it") } }
                }
                if (state.events.isNotEmpty()) {
                    Text("Transiciones recientes", style = MaterialTheme.typography.titleSmall)
                    state.events.takeLast(8).asReversed().forEach {
                        Text("${it.incidentId.toString().take(8)} · ${gatewayStateLabel(it.state)}")
                    }
                }
            }
        })
}

private fun gatewayStateLabel(state: GatewayState): String = when (state) {
    GatewayState.RECEIVED -> "SOS recibido por el gateway"
    GatewayState.QUEUED -> "Pendiente en cola"
    GatewayState.HTTP_STARTED -> "Envío HTTP iniciado; persistencia sin confirmar"
    GatewayState.LAB_CONFIRMED -> "API declara persistencia de laboratorio"
    GatewayState.ERROR -> "Error de conexión o TLS; sin confirmación"
    GatewayState.REJECTED -> "Rechazado; sin confirmación"
    GatewayState.TIMEOUT -> "Timeout; persistencia desconocida"
    GatewayState.UNKNOWN -> "Resultado desconocido; respuesta ausente o ambigua"
    GatewayState.EXPIRED -> "Pendiente vencido; sin confirmación"
    GatewayState.CANCELLED -> "Pendiente descartado al desactivar"
}

private fun admissionLabel(value: GatewayAdmission): String = when (value) {
    GatewayAdmission.QUEUED -> "SOS admitido en cola"
    GatewayAdmission.DISABLED -> "Gateway desactivado"
    GatewayAdmission.NOT_SOS -> "Mensaje de chat"
    GatewayAdmission.PRIVACY_BLOCKED -> "Bloqueado por política de privacidad"
    GatewayAdmission.INVALID -> "SOS inválido"
    GatewayAdmission.DUPLICATE -> "Incidente duplicado; sin nueva entrega"
    GatewayAdmission.CONFLICT -> "UUID con contenido conflictivo; copia rechazada"
    GatewayAdmission.QUEUE_FULL -> "Cola llena; incidente rechazado"
    GatewayAdmission.MEMORY_FULL -> "Memoria de deduplicación llena; incidente rechazado"
}
