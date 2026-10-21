package com.nicobutter.beaconchat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.ui.theme.BeaconEmergencyDark

/** Shared by Home and Transmit. Only explicit clicks request a local signal through the host. */
@Composable
fun QuickHelpActions(
    onEmergencyTrigger: (EmergencyType, EmergencyMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val actions = listOf(
        QuickHelpAction("SOS", "Ayuda urgente", EmergencyType.SOS, EmergencyMode.ALL),
        QuickHelpAction("HELP", "Auxilio", EmergencyType.HELP, EmergencyMode.ALL),
        QuickHelpAction("TRAPPED", "Atrapado", EmergencyType.TRAPPED, EmergencyMode.ALL),
        QuickHelpAction("DISCRETO", "Solo BLE", EmergencyType.SOS, EmergencyMode.DISCREET)
    )
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Señales rápidas de ayuda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Señalización local. No confirma recepción ni contacto con un servicio de emergencias.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BoxWithConstraints {
                val columns = if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    actions.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { action ->
                                QuickActionButton(action, enabled,
                                    { onEmergencyTrigger(action.type, action.mode) }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class QuickHelpAction(val label: String, val detail: String,
    val type: EmergencyType, val mode: EmergencyMode)

@Composable
private fun QuickActionButton(action: QuickHelpAction, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 76.dp),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(12.dp),
        colors = if (action.label == "SOS") ButtonDefaults.buttonColors(
            containerColor = BeaconEmergencyDark, contentColor = Color.White
        ) else ButtonDefaults.filledTonalButtonColors()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(action.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(action.detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
