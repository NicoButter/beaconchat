package com.nicobutter.beaconchat.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nicobutter.beaconchat.mesh.ble.MeshActivityKind
import com.nicobutter.beaconchat.mesh.ble.MeshBleSnapshot
import com.nicobutter.beaconchat.sos.*
import kotlinx.coroutines.delay
import java.util.UUID
import java.text.DateFormat
import java.util.Date

/** Real GPS stays in this local, non-saveable dialog; mesh uses separately created laboratory data. */
@Composable
fun SosLaboratoryDialog(mesh: MeshBleSnapshot, submit: (SosIncident) -> SosSubmission, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gps = remember(context, lifecycle) { AndroidGnssLocationSource(context, lifecycle) }
    val capture by gps.state.collectAsState()
    var resumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var pendingRequest by remember { mutableStateOf(false) }
    var timeoutSeconds by remember { mutableStateOf("30") }
    var requestTimeout by remember { mutableStateOf(30_000L) }
    var type by remember { mutableStateOf(EmergencyType.MEDICAL) }
    var severity by remember { mutableStateOf(DeclaredSeverity.HIGH) }
    var note by remember { mutableStateOf("") }
    var local by remember { mutableStateOf<SosIncident?>(null) }
    var laboratory by remember { mutableStateOf<SosIncident?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pendingRequest = true // The effect waits for RESUMED after the Android permission dialog.
    }
    DisposableEffect(gps, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed = true
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                resumed = false
                local = null // No retention/backups of a real GNSS SOS across background transitions.
                gps.cancel()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); gps.close() }
    }
    LaunchedEffect(pendingRequest, resumed) {
        if (pendingRequest && resumed) { pendingRequest = false; gps.request(requestTimeout) }
    }
    LaunchedEffect(gps, resumed) {
        while (resumed) { gps.refresh(); delay(1_000) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Laboratorio SOS offline · experimental") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar y descartar ubicación") } },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No es un servicio de rescate. GPS local no requiere Internet. La ubicación real queda solo en este diálogo y nunca se copia al ensayo mesh.")
                Text("Tipo de emergencia")
                EnumChoice(type, EmergencyType.values().toList(), { type = it }) { when (it) {
                    EmergencyType.MEDICAL -> "Médica"; EmergencyType.FIRE -> "Incendio"; EmergencyType.TRAPPED -> "Atrapamiento"; EmergencyType.OTHER -> "Otra"
                } }
                Text("Gravedad declarada (sin clasificación automática)")
                EnumChoice(severity, DeclaredSeverity.values().toList(), { severity = it }) { when (it) {
                    DeclaredSeverity.LOW -> "Baja"; DeclaredSeverity.MODERATE -> "Moderada"; DeclaredSeverity.HIGH -> "Alta"; DeclaredSeverity.CRITICAL -> "Crítica"
                } }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota solo local · máximo 256 bytes UTF-8") }, maxLines = 3)
                OutlinedTextField(value = timeoutSeconds, onValueChange = { timeoutSeconds = it }, label = { Text("Espera GPS: 1..120 segundos") }, singleLine = true)
                Button(enabled = resumed && capture.status != GnssStatus.SEARCHING, onClick = {
                    val seconds = timeoutSeconds.toLongOrNull()
                    if (seconds == null || seconds !in 1..120) feedback = "Espera GPS inválida"
                    else {
                        requestTimeout = seconds * 1_000
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            gps.request(requestTimeout)
                        } else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }) { Text("Solicitar ubicación GPS local") }
                if (capture.status == GnssStatus.SEARCHING) TextButton(onClick = { pendingRequest = false; gps.cancel() }) { Text("Cancelar captura") }
                Text(when (capture.status) {
                    GnssStatus.IDLE -> "Ubicación no solicitada"
                    GnssStatus.SEARCHING -> "Buscando GPS; no hay fix actual confirmado"
                    GnssStatus.CURRENT -> "Ubicación reciente disponible localmente"
                    GnssStatus.LAST_KNOWN -> "Última posición conocida; no es actual"
                    GnssStatus.NO_POSITION -> "Ubicación no disponible; se permite SOS sin posición"
                    GnssStatus.PERMISSION_REQUIRED -> "Sin permiso preciso; se permite SOS sin posición"
                    GnssStatus.CANCELLED -> "Captura cancelada; posición descartada"
                    GnssStatus.ERROR -> "Captura no disponible"
                })
                Text(capture.detail)
                capture.position?.let { Text(positionDescription(it, revealCoordinates = true)) }
                Button(onClick = {
                    try {
                        local = SosIncident(UUID.randomUUID(), type, severity, System.currentTimeMillis(), note.takeIf { it.isNotBlank() }, gps.positionForSos())
                        feedback = "SOS creado localmente; ubicación real y nota no habilitadas para mesh"
                    } catch (e: IllegalArgumentException) { feedback = e.message }
                }) { Text("Crear SOS local con posición disponible") }
                OutlinedButton(onClick = {
                    try {
                        local = SosIncident(UUID.randomUUID(), type, severity, System.currentTimeMillis(), note.takeIf { it.isNotBlank() })
                        feedback = "SOS creado localmente sin posición"
                    } catch (e: IllegalArgumentException) { feedback = e.message }
                }) { Text("Crear SOS local sin posición") }
                local?.let { SelectionContainer { Text("SOS local ${it.incidentId}\n${it.type} · ${it.severity}\n" +
                    "Creado: ${DateFormat.getDateTimeInstance().format(Date(it.createdAtEpochMillis))}\nPosición y edad al crear el SOS; no son seguimiento en vivo:\n" +
                    (it.position?.let { position -> positionDescription(position, true) } ?: "Sin coordenadas")) } }
                Divider()
                Text("Ensayos mesh: datos separados del SOS local; nota de prueba fija. Activá la participación en el panel Mesh experimental.")
                OutlinedButton(onClick = {
                    laboratory = SosMeshPolicy.laboratoryIncident(true, type, severity, System.currentTimeMillis())
                    feedback = "Ensayo sintético creado localmente; no transmitido"
                }) { Text("Preparar SOS sintético") }
                OutlinedButton(onClick = {
                    laboratory = SosMeshPolicy.laboratoryIncident(false, type, severity, System.currentTimeMillis())
                    feedback = "Ensayo sin posición creado localmente; no transmitido"
                }) { Text("Preparar ensayo sin posición") }
                laboratory?.let { incident ->
                    SelectionContainer { Text("SOS de laboratorio ${incident.incidentId}\n${incident.type} · ${incident.severity}\n" +
                        "Datos fijados al crear; un reintento conserva UUID y contenido.\n" +
                        (incident.position?.let { positionDescription(it, true) } ?: "Sin coordenadas")) }
                    Text("Contenido: ${SosCodec.encode(incident).toByteArray(Charsets.UTF_8).size}/1024 bytes · broadcast · TTL 3")
                    Button(enabled = mesh.enabled && resumed, onClick = {
                        feedback = when (submit(incident).status) {
                            SosSubmissionStatus.ORIGINATED_LOCALLY -> "Intento originado localmente; consultá cola/enlace. Destino final sin confirmar"
                            SosSubmissionStatus.REJECTED -> "Intento rechazado; destino final sin confirmar"
                            SosSubmissionStatus.PRIVACY_BLOCKED -> "Bloqueado por privacidad; no transmitido"
                        }
                    }) { Text("Transmitir / reintentar el mismo SOS de prueba") }
                }
                if (!mesh.enabled) Text("Mesh desactivado: podés crear SOS local; para ensayar envíos activá participación mesh.")
                feedback?.let { Text(it) }
                Text("Actividad SOS de laboratorio (máximo 8)")
                mesh.events.filter { it.message?.content?.startsWith(SosCodec.PREFIX) == true }.takeLast(8).reversed().forEach { event ->
                    val incident = runCatching { SosCodec.decode(event.message!!.content) }.getOrNull()
                    if (incident != null) SelectionContainer { Text("SOS ${incident.incidentId}\n" + when (event.kind) {
                        MeshActivityKind.QUEUED -> "Admitido en cola local"
                        MeshActivityKind.LINK_WRITTEN -> "Escrito en enlace; destino final sin confirmar"
                        MeshActivityKind.LOCAL_RECEIVED -> "SOS interpretado y recibido localmente; no confirma otros destinos"
                        MeshActivityKind.ORIGINATED -> "Intento originado localmente; no confirma admisión en cola"
                        MeshActivityKind.FAILURE -> "Fallo local; destino final sin confirmar"
                        MeshActivityKind.INFO -> event.detail
                    } + "\n${event.detail}\n${incident.type} · ${incident.severity}\n" +
                        (incident.position?.let { positionDescription(it, false) } ?: "Sin posición")) }
                }
                mesh.events.lastOrNull { it.kind == MeshActivityKind.FAILURE }?.let { Text(it.detail, color = MaterialTheme.colorScheme.error) }
            }
        })
}

@Composable
private fun <T> EnumChoice(selected: T, values: List<T>, onSelect: (T) -> Unit, label: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label(selected)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value -> DropdownMenuItem(text = { Text(label(value)) }, onClick = { onSelect(value); expanded = false }) }
        }
    }
}

private fun positionDescription(position: SosPosition, revealCoordinates: Boolean): String =
    (if (revealCoordinates) "${position.latitude}, ${position.longitude} · " else "") +
        "Fuente ${position.source} · ${position.validity} · edad ${position.ageMillis / 1_000} s · " +
        "precisión estimada ${position.accuracyMeters?.let { "$it m" } ?: "desconocida"}"
