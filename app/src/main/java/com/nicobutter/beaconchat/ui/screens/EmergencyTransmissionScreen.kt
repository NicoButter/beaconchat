package com.nicobutter.beaconchat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.SoundController
import com.nicobutter.beaconchat.transceiver.VibrationController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Emergency transmission types with their visual characteristics.
 */
enum class EmergencyType(
    val displayName: String,
    val backgroundColor: Color,
    val icon: String
) {
    SOS("SOS", Color(0xFFD32F2F), "🆘"),
    HELP("AUXILIO", Color(0xFFFF6F00), "⚠️"),
    OK("TODO BIEN", Color(0xFF388E3C), "✅"),
    LOCATION("MI UBICACIÓN", Color(0xFF1976D2), "📍")
}

/**
 * Transmission method for emergency signals.
 */
enum class EmergencyMethod(
    val displayName: String,
    val icon: String,
    val description: String
) {
    LIGHT("Luz", "💡", "Transmitiendo con linterna"),
    VIBRATION("Vibración", "📳", "Transmitiendo con vibración"),
    SOUND("Ultrasonido", "🔊", "Transmitiendo con ultrasonido"),
    ALL("Todo", "🌟", "Transmitiendo con todos los métodos")
}

/**
 * Emergency transmission screen with fullscreen visual feedback.
 *
 * Displays a prominent emergency transmission interface with animated
 * visual feedback, transmission status, elapsed time, and cancel button.
 * Designed for high-visibility emergency situations.
 *
 * @param emergencyType Type of emergency signal being transmitted
 * @param method Transmission method being used
 * @param flashlightController Controller for light-based transmission
 * @param vibrationController Controller for vibration-based transmission
 * @param soundController Controller for sound-based transmission
 * @param morseEncoder Encoder for Morse code signals
 * @param onDismiss Callback invoked when transmission is cancelled
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun EmergencyTransmissionScreen(
    emergencyType: EmergencyType,
    method: EmergencyMethod,
    flashlightController: FlashlightController,
    vibrationController: VibrationController,
    soundController: SoundController,
    morseEncoder: MorseEncoder,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var elapsedSeconds by remember { mutableStateOf(0) }
    var transmissionCount by remember { mutableStateOf(0) }
    
    // Pulsating animation synchronized with transmission
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
    
    // Timer counter
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000)
            elapsedSeconds++
        }
    }
    
    // Continuous transmission
    LaunchedEffect(Unit) {
        val message = when (emergencyType) {
            EmergencyType.SOS -> "SOS"
            EmergencyType.HELP -> "HELP"
            EmergencyType.OK -> "OK"
            EmergencyType.LOCATION -> "HERE"
        }
        
        val timings = morseEncoder.encode(message)
        
        while (isActive) {
            try {
                when (method) {
                    EmergencyMethod.LIGHT -> {
                        flashlightController.transmit(timings)
                    }
                    EmergencyMethod.VIBRATION -> {
                        vibrationController.transmit(timings)
                    }
                    EmergencyMethod.SOUND -> {
                        soundController.transmit(timings)
                    }
                    EmergencyMethod.ALL -> {
                        launch { flashlightController.transmit(timings) }
                        launch { vibrationController.transmit(timings) }
                        launch { soundController.transmit(timings) }
                    }
                }
                transmissionCount++
                delay(500) // Short pause between transmissions
            } catch (e: Exception) {
                // Continue transmission even if one method fails
            }
        }
    }
    
    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            flashlightController.stop()
            vibrationController.cleanup()
            soundController.cleanup()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(emergencyType.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Close button at top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Large pulsating emergency icon
            Text(
                text = emergencyType.icon,
                fontSize = 120.sp,
                modifier = Modifier
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Emergency type
            Text(
                text = "EMITIENDO",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = emergencyType.displayName,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Method indicator with icon
            Card(
                modifier = Modifier.padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = method.icon,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = method.description,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Método: ${method.displayName}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // Statistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Elapsed time
                StatCard(
                    label = "Tiempo",
                    value = formatTime(elapsedSeconds),
                    icon = "⏱️"
                )
                
                // Transmission count
                StatCard(
                    label = "Señales",
                    value = "$transmissionCount",
                    icon = "📡"
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Large cancel button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = emergencyType.backgroundColor
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "DETENER TRANSMISIÓN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Text(
                text = "Los drones y dispositivos cercanos pueden detectar esta señal",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
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
