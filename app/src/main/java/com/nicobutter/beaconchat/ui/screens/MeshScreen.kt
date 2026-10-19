package com.nicobutter.beaconchat.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nicobutter.beaconchat.mesh.DirectBleProtocol
import com.nicobutter.beaconchat.data.UserPreferences
import com.nicobutter.beaconchat.mesh.BLEMeshController
import com.nicobutter.beaconchat.mesh.ChatMessage
import com.nicobutter.beaconchat.mesh.MeshPeer
import com.nicobutter.beaconchat.mesh.ble.MeshBleSnapshot
import com.nicobutter.beaconchat.mesh.ble.MeshActivityKind
import com.nicobutter.beaconchat.mesh.core.NodeId
import java.util.UUID
import com.nicobutter.beaconchat.sos.SosCodec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Mesh networking screen for Bluetooth LE-based peer-to-peer communication.
 *
 * Provides interface for discovering peers, sending messages, and managing
 * mesh network connections. Displays connected peers, message history,
 * and handles Bluetooth permissions and state management.
 *
 * @param meshController Bluetooth LE mesh controller for network operations
 * @param userPreferences User preferences for callsign and settings
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun MeshScreen(
        meshController: BLEMeshController,
        userPreferences: UserPreferences,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val peers by meshController.peers.collectAsState()
    val messages by meshController.messages.collectAsState()
    val isAdvertising by meshController.isAdvertising.collectAsState()
    val isScanning by meshController.isScanning.collectAsState()
    val scanRequested by meshController.scanRequested.collectAsState()
    val isStartingAdvertising by meshController.isStartingAdvertising.collectAsState()
    val connectionStatus by meshController.connectionStatus.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }
    val bluetoothEnabled by meshController.bluetoothEnabled.collectAsState()
    val experimentalState by meshController.experimentalMesh.state.collectAsState()
    var showExperimental by remember { mutableStateOf(false) }
    var showSos by remember { mutableStateOf(false) }

    val callsign by userPreferences.callsign.collectAsState(initial = "UNKNOWN")

    var selectedPeer by remember { mutableStateOf<MeshPeer?>(null) }

    // Permissions handling
    val permissionsToRequest =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

    var hasPermissions by remember {
        mutableStateOf(
                permissionsToRequest.all {
                    ContextCompat.checkSelfPermission(context, it) ==
                            PackageManager.PERMISSION_GRANTED
                }
        )
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { _ ->
                        hasPermissions = permissionsToRequest.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                        if (hasPermissions) {
                            meshController.updateBluetoothState()
                        }
                    }
            )

    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Actualizar estado después de intentar activar Bluetooth
        meshController.updateBluetoothState()
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(permissionsToRequest)
        } else {
            meshController.updateBluetoothState()
        }
    }

    val isSending by meshController.isSending.collectAsState()
    val lastError by meshController.lastError.collectAsState()

    // Estado para controlar si el usuario quiere auto-escaneo
    var autoScanEnabled by remember { mutableStateOf(true) }

    var visibilityEnabled by remember { mutableStateOf(true) }
    LaunchedEffect(hasPermissions, bluetoothEnabled, autoScanEnabled, visibilityEnabled, callsign, resumed, experimentalState.enabled) {
        if (hasPermissions && bluetoothEnabled && resumed) {
            if (autoScanEnabled || experimentalState.enabled) meshController.startScanning() else meshController.stopScanning()
            if (visibilityEnabled || experimentalState.enabled) meshController.startAdvertising(callsign) else meshController.stopAdvertising()
        } else meshController.stop()
    }

    DisposableEffect(lifecycleOwner, meshController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasPermissions = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    meshController.resume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> { resumed = false; meshController.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            meshController.resume()
            resumed = true
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            meshController.pause()
        }
    }

    if (selectedPeer != null) {
        ChatScreen(
                peer = selectedPeer!!,
                messages =
                        messages.filter {
                            // Mensajes enviados a este peer por mí
                            (it.isFromMe && it.recipientId == selectedPeer!!.id) ||
                            // Mensajes recibidos de este peer
                            (it.senderId == selectedPeer!!.id && !it.isFromMe)
                        },
                isSending = isSending,
                lastError = lastError,
                callsign = callsign,
                connectionStatus = connectionStatus,
                onSendMessage = { content ->
                    meshController.sendMessage(selectedPeer!!.id, content, callsign)
                },
                onBack = { meshController.cancelSend(); selectedPeer = null }
        )
    } else {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mesh Radar")
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                            actions = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (bluetoothEnabled) {
                                        Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Bluetooth On",
                                                tint = Color(0xFF2E7D32)
                                        )
                                    } else {
                                        Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Bluetooth Off",
                                                tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                    )
                }
        ) { paddingValues ->
            Column(modifier = modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                // Status Card
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Mi Estado",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                            text = "Callsign: $callsign",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                            text =
                                                    if (isAdvertising) "Visible (Anunciando)"
                                                    else if (isStartingAdvertising) "Iniciando publicidad/GATT…" else "Invisible",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isAdvertising) Color(0xFF2E7D32) else Color.Gray
                                    )
                                }
                                Switch(
                                        checked = isAdvertising || isStartingAdvertising,
                                        onCheckedChange = { enabled ->
                                            visibilityEnabled = enabled
                                            if (enabled) {
                                                meshController.startAdvertising(callsign)
                                            } else {
                                                meshController.stopAdvertising()
                                            }
                                        },
                                        enabled = hasPermissions && bluetoothEnabled && !experimentalState.enabled
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Botones de control de escaneo
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                        onClick = { 
                                            autoScanEnabled = true
                                            meshController.startScanning() 
                                        },
                                        enabled = hasPermissions && bluetoothEnabled && !scanRequested,
                                        modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Iniciar Escaneo")
                                }
                                
                                OutlinedButton(
                                        onClick = { 
                                            autoScanEnabled = false
                                            meshController.stopScanning() 
                                        },
                                        enabled = hasPermissions && bluetoothEnabled && scanRequested && !experimentalState.enabled,
                                        modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Detener")
                                }
                            }
                        }
                    }
                }

                OutlinedButton(onClick = { showExperimental = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Mesh experimental: ${if (experimentalState.enabled) "activo" else "inactivo"} · ${experimentalState.queued} en cola")
                }
                OutlinedButton(onClick = { showSos = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Laboratorio SOS offline")
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (lastError != null) {
                    Text(lastError!!, color = MaterialTheme.colorScheme.error)
                }
                Text(if (isScanning) "Señales BLE detectadas" else if (scanRequested) "Escaneo solicitado; esperando señales" else "Escaneo detenido")

                // Peers List
                Text(
                        text = "Dispositivos Cercanos (${peers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!hasPermissions) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Se requieren permisos de Bluetooth para ver dispositivos.")
                    }
                } else if (!bluetoothEnabled) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Bluetooth Off",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    "Bluetooth desactivado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    "Activa Bluetooth para usar el Mesh Radar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                    onClick = {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        bluetoothEnableLauncher.launch(enableBtIntent)
                                    }
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activar Bluetooth")
                            }
                        }
                    }
                } else if (peers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (scanRequested) CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    if (scanRequested) "Esperando señales BLE…" else "Escaneo detenido",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(peers) { peer -> PeerItem(peer, onClick = { selectedPeer = peer }) }
                    }
                }
            }
        }
    }
    if (showExperimental) {
        ExperimentalMeshDialog(experimentalState, hasPermissions && bluetoothEnabled && resumed,
            onEnabled = meshController::setMeshParticipation,
            onSend = { destination, text, ttl -> meshController.experimentalMesh.originate(destination, text, ttl) },
            onDismiss = { showExperimental = false })
    }
    if (showSos) {
        SosLaboratoryDialog(experimentalState, meshController.experimentalMesh::submitSosLaboratory) { showSos = false }
    }
}

@Composable
private fun ExperimentalMeshDialog(state: MeshBleSnapshot, available: Boolean,
    onEnabled: (Boolean) -> Unit, onSend: (NodeId?, String, Int) -> Boolean, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("Prueba mesh sin Internet 🆘") }
    var destination by remember { mutableStateOf("") }
    var ttl by remember { mutableStateOf("3") }
    var inputError by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Mesh experimental") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Solo en primer plano. Al salir o pausar se desactiva. Mantiene publicidad y escaneo mientras participa.")
                SelectionContainer { Text("NodeId local: ${state.nodeId?.value ?: "Inicializando…"}") }
                Text("Identidades declaradas sin autenticar. Sin cifrado ni confirmación del destino final.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Participar", Modifier.weight(1f))
                    Switch(checked = state.enabled, onCheckedChange = onEnabled,
                        enabled = available && state.nodeId != null)
                }
                Text("Estado local: ${state.phase} · cola ${state.queued}/8 · sesiones entrantes ${state.incoming}/8")
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Mensaje (máximo 1024 bytes UTF-8)") }, maxLines = 4)
                OutlinedTextField(value = destination, onValueChange = { destination = it },
                    label = { Text("Destino UUID; vacío = broadcast") }, maxLines = 2)
                OutlinedTextField(value = ttl, onValueChange = { ttl = it }, label = { Text("Saltos (0..16)") }, singleLine = true)
                Button(enabled = state.enabled, onClick = {
                    try {
                        val target = destination.trim().takeIf { it.isNotEmpty() }?.let { NodeId(UUID.fromString(it)) }
                        val hops = ttl.toIntOrNull()
                        require(hops != null && hops in 0..16) { "TTL debe estar entre 0 y 16" }
                        inputError = if (onSend(target, text, hops)) null else "No admitido; consultá el estado local"
                    } catch (e: IllegalArgumentException) { inputError = e.message ?: "Destino/TTL inválido" }
                }) { Text("Originar prueba · sin confirmar") }
                inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Vecinos (${state.neighbors.count { it.nodeId != null }} con mesh verificado)", fontWeight = FontWeight.Bold)
                state.neighbors.forEach { neighbor ->
                    SelectionContainer { Text("${neighbor.address}\n${neighbor.nodeId?.value ?: "Sin NodeId verificado"}\n${neighbor.state}") }
                }
                Text("Actividad local (últimos 8 eventos)", fontWeight = FontWeight.Bold)
                state.events.takeLast(8).reversed().forEach { event ->
                    val label = when (event.kind) {
                        MeshActivityKind.ORIGINATED -> "Originado localmente"
                        MeshActivityKind.LOCAL_RECEIVED -> "Recibido localmente"
                        MeshActivityKind.QUEUED -> "En cola local"
                        MeshActivityKind.LINK_WRITTEN -> "Escrito en enlace · no confirmado"
                        MeshActivityKind.FAILURE -> "Fallo local · no confirmado"
                        MeshActivityKind.INFO -> "Estado local"
                    }
                    SelectionContainer { Text("$label\n${event.detail}" + (event.message?.let {
                        val sos = if (it.content.startsWith(SosCodec.PREFIX)) runCatching { SosCodec.decode(it.content) }.getOrNull() else null
                        "\nOrigen: ${it.originId.value}\nMensaje: ${it.messageId.value} · TTL ${it.hopsRemaining}\n" +
                            (sos?.let { incident -> "SOS laboratorio ${incident.incidentId}: ${incident.type}, ${incident.severity}; " +
                                if (incident.position == null) "sin posición" else "posición sintética" } ?: it.content)
                    } ?: ""), color = if (event.kind == MeshActivityKind.FAILURE) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface) }
                    Divider()
                }
            }
        })
}

@Composable
fun PeerItem(peer: MeshPeer, onClick: () -> Unit) {
    Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = peer.callsign,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = peer.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "Toca para chatear",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            Icons.Default.Info,
                            contentDescription = "Signal",
                            modifier = Modifier.size(16.dp),
                            tint =
                                    when (peer.getSignalQuality()) {
                                        "Excelente" -> Color(0xFF2E7D32)
                                        "Buena" -> Color(0xFF558B2F)
                                        "Regular" -> Color(0xFFF9A825)
                                        else -> Color(0xFFC62828)
                                    }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                            text = "${peer.rssi} dBm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                    )
                }
                Text(
                        text = peer.timeSinceLastSeen(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Ícono de chat
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                    Icons.Default.Send,
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
        peer: MeshPeer,
        messages: List<ChatMessage>,
        isSending: Boolean = false,
        lastError: String? = null,
        callsign: String = "UNKNOWN",
        connectionStatus: String = "Sin conexión",
        onSendMessage: (String) -> Unit,
        onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val validationError = try {
        if (messageText.isNotEmpty()) DirectBleProtocol.encode(callsign, messageText)
        null
    } catch (e: Exception) { e.message ?: "Texto inválido" }
    val limit = try { DirectBleProtocol.messageLimit(callsign) } catch (_: Exception) { 0 }
    var pendingText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(messages) {
        if (pendingText != null && messages.lastOrNull()?.let { it.isFromMe && it.content == pendingText } == true) {
            messageText = ""
            pendingText = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                            text = peer.callsign,
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    if (isSending) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Text(
                                        text = peer.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(connectionStatus, modifier = Modifier.padding(horizontal = 8.dp))
                    // Mostrar error si hay uno
                    if (lastError != null) {
                        Row(
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                    text = lastError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Mensaje...") },
                                supportingText = { Text(validationError ?: "${messageText.toByteArray(Charsets.UTF_8).size}/$limit bytes UTF-8") },
                                isError = validationError != null,
                                shape = RoundedCornerShape(24.dp),
                                enabled = !isSending
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        pendingText = messageText
                                        onSendMessage(messageText)
                                    }
                                },
                                enabled = messageText.isNotBlank() && validationError == null && !isSending,
                                colors =
                                        IconButtonDefaults.iconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                        ) { 
                            if (isSending) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
    ) { paddingValues ->
        LazyColumn(
                state = listState,
                modifier =
                        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { items(messages) { message -> MessageBubble(message) } }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val color =
            if (message.isFromMe) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer

    val textColor =
            if (message.isFromMe) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
                modifier =
                        Modifier.background(color, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
        ) {
            if (!message.isFromMe) {
                Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                )
            }
            if (message.isFromMe) Text("Escritura GATT confirmada · sin ACK", style = MaterialTheme.typography.labelSmall)
            Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
            )
            Text(
                    text =
                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
