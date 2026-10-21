package com.nicobutter.beaconchat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicobutter.beaconchat.controller.EmergencyManager
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Emergency transmission screen with fullscreen visual feedback.
 *
 * The UI has one job: show status and let the user cancel.
 * All transmission logic is delegated to [EmergencyManager]:
 * ```
 * emergencyManager.startEmergency(type, mode)
 * ```
 *
 * @param emergencyType The semantic type of emergency to broadcast.
 * @param mode How the signal is transmitted (channels).
 * @param emergencyManager Orchestrator that drives the actual emitters.
 * @param onDismiss Callback invoked when the user cancels.
 */
@Composable
fun EmergencyTransmissionScreen(
    emergencyType: EmergencyType,
    mode: EmergencyMode,
    emergencyManager: EmergencyManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableStateOf(0) }

    // Local elapsed-time counter
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // Start transmission via manager; stop on leave
    DisposableEffect(emergencyType, mode) {
        emergencyManager.startEmergency(emergencyType, mode)
        onDispose { emergencyManager.stopEmergency() }
    }

    EmergencyTransmissionContent(emergencyType, mode, elapsedSeconds, onDismiss, modifier)
}

/** Presentation only, also used by device UI tests without activating any emitters. */
@Composable
internal fun EmergencyTransmissionContent(
    emergencyType: EmergencyType,
    mode: EmergencyMode,
    elapsedSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Keep the type's hue with enough contrast for white labels, including HELP's orange.
    val backgroundColor = lerp(MaterialTheme.colorScheme.background, Color(emergencyType.colorArgb), 0.55f)
    BoxWithConstraints(modifier.fillMaxSize().background(backgroundColor)) {
        val compact = maxHeight < 500.dp
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Señalización local", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                    Icon(Icons.Default.Close, contentDescription = "Detener y volver a Inicio", tint = Color.White)
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(emergencyType.icon, fontSize = if (compact) 56.sp else 96.sp,
                    modifier = Modifier.scale(pulseScale).alpha(pulseAlpha))
                Text(emergencyType.displayName, style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Modo: ${mode.displayName}", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(mode.description, color = Color.White)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Tiempo", formatTime(elapsedSeconds), "⏱️", Modifier.weight(1f))
                    StatCard("Canales solicitados", if (mode == EmergencyMode.ALL) "4" else "1", mode.icon, Modifier.weight(1f))
                }
                Text("No confirma recepción, entrega ni contacto con un servicio de emergencias.",
                    style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6B0000)),
                shape = RoundedCornerShape(16.dp)) {
                Text("DETENER TRANSMISIÓN", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * Statistics card for emergency transmission screen.
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Formats elapsed time in MM:SS format.
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
