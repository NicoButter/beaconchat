package com.nicobutter.beaconchat.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nicobutter.beaconchat.data.UserPreferences
import com.nicobutter.beaconchat.mesh.BLEMeshController
import com.nicobutter.beaconchat.mesh.MeshPeer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshScreen(
        meshController: BLEMeshController,
        userPreferences: UserPreferences,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val peers by meshController.peers.collectAsState()
    val isAdvertising by meshController.isAdvertising.collectAsState()
    val isScanning by meshController.isScanning.collectAsState()
    val bluetoothEnabled by meshController.bluetoothEnabled.collectAsState()

    val callsign by userPreferences.callsign.collectAsState(initial = "UNKNOWN")

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
                    onResult = { perms ->
                        hasPermissions = perms.values.all { it }
                        if (hasPermissions) {
                            meshController.updateBluetoothState()
                        }
                    }
            )

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            launcher.launch(permissionsToRequest)
        }
    }

    // Auto-start if permissions granted
    LaunchedEffect(hasPermissions, bluetoothEnabled) {
        if (hasPermissions && bluetoothEnabled) {
            if (!isScanning) meshController.startScanning()
            if (!isAdvertising && callsign.isNotBlank()) meshController.startAdvertising(callsign)
        }
    }

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
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        titleContentColor =
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                        actions = {
                            if (bluetoothEnabled) {
                                Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Bluetooth On",
                                        tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Bluetooth Off",
                                        tint = MaterialTheme.colorScheme.error
                                )
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
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                            else "Invisible",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isAdvertising) Color(0xFF2E7D32) else Color.Gray
                            )
                        }
                        Switch(
                                checked = isAdvertising,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        meshController.startAdvertising(callsign)
                                        meshController.startScanning()
                                    } else {
                                        meshController.stopAdvertising()
                                        meshController.stopScanning()
                                    }
                                },
                                enabled = hasPermissions && bluetoothEnabled
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    Text("Activa Bluetooth para usar el Radar.")
                }
            } else if (peers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                "Escaneando...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(peers) { peer -> PeerItem(peer) }
                }
            }
        }
    }
}

@Composable
fun PeerItem(peer: MeshPeer) {
    Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
        }
    }
}
