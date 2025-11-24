package com.nicobutter.beaconchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
// Use a conservative set of icons that are available in the project's icon set
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import com.nicobutter.beaconchat.transceiver.BinaryEncoder
import com.nicobutter.beaconchat.transceiver.VibrationController
import com.nicobutter.beaconchat.transceiver.SoundController
import com.nicobutter.beaconchat.data.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TransmissionMethod {
    FLASHLIGHT,
    VIBRATION,
    SOUND
}

enum class EncodingType {
    MORSE,
    ASCII_BINARY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmitterScreen(
    flashlightController: FlashlightController,
    vibrationController: VibrationController,
    soundController: SoundController,
    morseEncoder: MorseEncoder,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isTransmitting by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(TransmissionMethod.FLASHLIGHT) }
    var selectedEncoding by remember { mutableStateOf(EncodingType.MORSE) }
    var transmissionJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val binaryEncoder = remember { BinaryEncoder() }

    fun startContinuousTransmission(message: String) {
        if (message.isNotBlank()) {
            isTransmitting = true
            transmissionJob = scope.launch {
                val timings = when (selectedEncoding) {
                    EncodingType.MORSE -> morseEncoder.encode(message)
                    EncodingType.ASCII_BINARY -> binaryEncoder.encode(message)
                }
                while (isActive && isTransmitting) {
                    when (selectedMethod) {
                        TransmissionMethod.FLASHLIGHT -> flashlightController.transmit(timings)
                        TransmissionMethod.VIBRATION -> { /* TODO */ }
                        TransmissionMethod.SOUND -> { /* TODO */ }
                    }
                    kotlinx.coroutines.delay(500)
                }
            }
        }
    }

    fun stopTransmission() {
        transmissionJob?.cancel()
        transmissionJob = null
        isTransmitting = false
    }

    fun sendOnce(message: String) {
        if (message.isNotBlank()) {
            isTransmitting = true
            scope.launch {
                val timings = when (selectedEncoding) {
                    EncodingType.MORSE -> morseEncoder.encode(message)
                    EncodingType.ASCII_BINARY -> binaryEncoder.encode(message)
                }
                when (selectedMethod) {
                    TransmissionMethod.FLASHLIGHT -> flashlightController.transmit(timings)
                    TransmissionMethod.VIBRATION -> { /* TODO */ }
                    TransmissionMethod.SOUND -> { /* TODO */ }
                }
                isTransmitting = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Transmitir", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Envía mensajes con linterna, vibración o sonido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick messages
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Mensajes rápidos", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionButton(label = "SOS", icon = Icons.Default.Warning, onClick = { startContinuousTransmission("SOS") }, enabled = !isTransmitting, modifier = Modifier.weight(1f).height(48.dp))
                    QuickActionButton(label = "AUXILIO", icon = Icons.Default.Info, onClick = { startContinuousTransmission("AUXILIO") }, enabled = !isTransmitting, modifier = Modifier.weight(1f).height(48.dp))
                    QuickActionButton(label = "AYUDA", icon = Icons.Default.Info, onClick = { startContinuousTransmission("AYUDA") }, enabled = !isTransmitting, modifier = Modifier.weight(1f).height(48.dp))
                    QuickActionButton(label = "OK", icon = Icons.Default.Info, onClick = { startContinuousTransmission("OK") }, enabled = !isTransmitting, modifier = Modifier.weight(1f).height(48.dp))
                }

            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom message
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Mensaje Personalizado", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Escribe tu mensaje") }, modifier = Modifier.fillMaxWidth(), enabled = !isTransmitting, maxLines = 3)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { sendOnce(text) }, enabled = !isTransmitting && text.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Enviar una vez") }
                    Button(onClick = { startContinuousTransmission(text) }, enabled = !isTransmitting && text.isNotBlank(), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Text("Transmitir continuo") }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Encoding selection
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedEncoding == EncodingType.MORSE, onClick = { selectedEncoding = EncodingType.MORSE }, label = { Text("Código Morse") }, leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }, modifier = Modifier.weight(1f))
            FilterChip(selected = selectedEncoding == EncodingType.ASCII_BINARY, onClick = { selectedEncoding = EncodingType.ASCII_BINARY }, label = { Text("ASCII Binario") }, leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Method selection
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MethodToggle(icon = Icons.Default.Warning, label = "Linterna", selected = selectedMethod == TransmissionMethod.FLASHLIGHT, onClick = { selectedMethod = TransmissionMethod.FLASHLIGHT }, enabled = !isTransmitting, modifier = Modifier.weight(1f))
                MethodToggle(icon = Icons.Default.Info, label = "Vibración", selected = selectedMethod == TransmissionMethod.VIBRATION, onClick = { selectedMethod = TransmissionMethod.VIBRATION }, enabled = !isTransmitting, modifier = Modifier.weight(1f))
                MethodToggle(icon = Icons.Default.Warning, label = "Sonido", selected = selectedMethod == TransmissionMethod.SOUND, onClick = { selectedMethod = TransmissionMethod.SOUND }, enabled = !isTransmitting, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isTransmitting, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { stopTransmission() }, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = "Detener", modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DETENER", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(40.dp), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Transmitiendo...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun QuickActionButton(label: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun MethodToggle(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(56.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
            Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
