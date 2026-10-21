package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.ui.components.QuickHelpActions
import com.nicobutter.beaconchat.ui.theme.BeaconChatTheme

/** Functional entry point. Rendering Home never activates a sensor, permission request or emitter. */
@Composable
fun HomeScreen(
    onNavigateToTransmit: () -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToMesh: () -> Unit,
    onEmergencyTrigger: (EmergencyType, EmergencyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("home"),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("¿Qué necesitás hacer?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            item { QuickHelpActions(onEmergencyTrigger) }
            item {
                HomeAction("Emitir señales o mensajes", "Linterna, vibración o sonido", Icons.Default.Send, onNavigateToTransmit)
            }
            item {
                HomeAction("Recibir o detectar señales", "Cámara, QR y detección de vibración", Icons.Default.Search, onNavigateToReceive)
            }
            item {
                HomeAction("Mesh Radar · experimental", "Chat entre vecinos y laboratorio SOS sintético. Sin entrega garantizada.",
                    Icons.Default.Share, onNavigateToMesh)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAction(title: String, detail: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedCard(onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp).heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(name = "Inicio 360", widthDp = 360, heightDp = 800)
@Preview(name = "Inicio 390", widthDp = 390, heightDp = 844)
@Preview(name = "Inicio texto grande", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun HomePreview() { BeaconChatTheme { HomeScreen({}, {}, {}, { _, _ -> }) } }
