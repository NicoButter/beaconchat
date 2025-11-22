package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

enum class TransmissionMethod {
    FLASHLIGHT,
    VIBRATION,
    SOUND
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmitterScreen(
        flashlightController: FlashlightController,
        morseEncoder: MorseEncoder,
        modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isTransmitting by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(TransmissionMethod.FLASHLIGHT) }
    var transmissionJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // Función para transmitir continuamente
    fun startContinuousTransmission(message: String) {
        if (message.isNotBlank()) {
            isTransmitting = true
            transmissionJob = scope.launch {
                val timings = morseEncoder.encode(message)
                while (isActive && isTransmitting) {
                    when (selectedMethod) {
                        TransmissionMethod.FLASHLIGHT -> flashlightController.transmit(timings)
                        TransmissionMethod.VIBRATION -> { /* TODO: Implementar vibración */ }
                        TransmissionMethod.SOUND -> { /* TODO: Implementar sonido */ }
                    }
                    // Pequeña pausa entre repeticiones
                    kotlinx.coroutines.delay(500)
                }
            }
        }
    }

    // Función para detener la transmisión
    fun stopTransmission() {
        transmissionJob?.cancel()
        transmissionJob = null
        isTransmitting = false
    }

    // Función para enviar mensaje una sola vez
    fun sendOnce(message: String) {
        if (message.isNotBlank()) {
            isTransmitting = true
            scope.launch {
                val timings = morseEncoder.encode(message)
                when (selectedMethod) {
                    TransmissionMethod.FLASHLIGHT -> flashlightController.transmit(timings)
                    TransmissionMethod.VIBRATION -> { /* TODO: Implementar vibración */ }
                    TransmissionMethod.SOUND -> { /* TODO: Implementar sonido */ }
                }
                isTransmitting = false
            }
        }
    }

    Column(
            modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
    ) {
        Text(
                text = "Transmitir Mensaje",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mensajes predefinidos
        Text(
                text = "Mensajes Rápidos (Continuo)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
        )
        Text(
                text = "Toca para emitir continuamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                    onClick = { startContinuousTransmission("SOS") },
                    modifier = Modifier.weight(1f),
                    enabled = !isTransmitting,
                    colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                    )
            ) {
                Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("SOS")
            }

            FilledTonalButton(
                    onClick = { startContinuousTransmission("AUXILIO") },
                    modifier = Modifier.weight(1f),
                    enabled = !isTransmitting
            ) { Text("AUXILIO") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                    onClick = { startContinuousTransmission("AYUDA") },
                    modifier = Modifier.weight(1f),
                    enabled = !isTransmitting
            ) { Text("AYUDA") }

            FilledTonalButton(
                    onClick = { startContinuousTransmission("OK") },
                    modifier = Modifier.weight(1f),
                    enabled = !isTransmitting
            ) {
                Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("OK")
            }
        }

        // Botón STOP (visible solo cuando se está transmitiendo)
        if (isTransmitting) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                    onClick = { stopTransmission() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Detener",
                        modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("DETENER", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mensaje personalizado
        Text(
                text = "Mensaje Personalizado",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Escribir mensaje") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTransmitting,
                maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Método de transmisión
        Text(
                text = "Método de Transmisión",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                    selected = selectedMethod == TransmissionMethod.FLASHLIGHT,
                    onClick = { selectedMethod = TransmissionMethod.FLASHLIGHT },
                    label = { Text("💡 Linterna") },
                    enabled = !isTransmitting,
                    modifier = Modifier.weight(1f)
            )

            FilterChip(
                    selected = selectedMethod == TransmissionMethod.VIBRATION,
                    onClick = { selectedMethod = TransmissionMethod.VIBRATION },
                    label = { Text("📳 Vibración") },
                    enabled = !isTransmitting,
                    modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilterChip(
                selected = selectedMethod == TransmissionMethod.SOUND,
                onClick = { selectedMethod = TransmissionMethod.SOUND },
                label = { Text("🔊 Sonido") },
                enabled = !isTransmitting,
                modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de enviar una sola vez
        Button(
                onClick = { sendOnce(text) },
                enabled = !isTransmitting && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                    "Enviar Una Vez",
                    style = MaterialTheme.typography.titleMedium
            )
        }

        if (isTransmitting) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Transmitiendo...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
