package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.data.UserPreferences
import com.nicobutter.beaconchat.transceiver.BinaryEncoder
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import com.nicobutter.beaconchat.transceiver.QRGenerator
import com.nicobutter.beaconchat.transceiver.SoundController
import com.nicobutter.beaconchat.transceiver.VibrationController
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TransmissionMethod {
        FLASHLIGHT,
        VIBRATION,
        SOUND,
        QR_CODE
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
        val qrGenerator = remember { QRGenerator() }

        // Callsign state from UserPreferences
        val callsign by userPreferences.callsign.collectAsState(initial = "")
        val callsignEnabled by userPreferences.callsignEnabled.collectAsState(initial = false)

        // QR transmission state
        var showQRTransmission by remember { mutableStateOf(false) }
        var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

        // Estados para debugging visual
        var flashlightState by remember { mutableStateOf(false) }
        var pulseCount by remember { mutableStateOf(0) }
        var totalPulses by remember { mutableStateOf(0) }
        var encodedMessage by remember { mutableStateOf("") }

        // Configurar callback de debugging
        DisposableEffect(flashlightController) {
                flashlightController.onStateChange = { isOn, current, total ->
                        flashlightState = isOn
                        pulseCount = current
                        totalPulses = total
                }
                onDispose { flashlightController.onStateChange = null }
        }

        // Función para transmitir continuamente
        fun startContinuousTransmission(message: String) {
                if (message.isNotBlank()) {
                        isTransmitting = true
                        transmissionJob =
                                scope.launch {
                                        // Format message with callsign
                                        val formattedMessage =
                                                userPreferences.formatMessageWithCallsign(
                                                        message,
                                                        callsign,
                                                        callsignEnabled
                                                )

                                        val timings =
                                                when (selectedEncoding) {
                                                        EncodingType.MORSE ->
                                                                morseEncoder.encode(
                                                                        formattedMessage
                                                                )
                                                        EncodingType.ASCII_BINARY ->
                                                                binaryEncoder.encode(
                                                                        formattedMessage
                                                                )
                                                }
                                        while (isActive && isTransmitting) {
                                                when (selectedMethod) {
                                                        TransmissionMethod.FLASHLIGHT ->
                                                                flashlightController.transmit(
                                                                        timings
                                                                )
                                                        TransmissionMethod.VIBRATION ->
                                                                vibrationController.transmit(
                                                                        timings
                                                                )
                                                        TransmissionMethod.SOUND ->
                                                                soundController.transmit(timings)
                                                        TransmissionMethod.QR_CODE -> {
                                                                // QR doesn't use continuous
                                                                // transmission
                                                        }
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
                flashlightState = false
                pulseCount = 0
                totalPulses = 0
        }

        // Función para enviar mensaje una sola vez
        fun sendOnce(message: String) {
                if (message.isNotBlank()) {
                        scope.launch {
                                // Format message with callsign
                                val formattedMessage =
                                        userPreferences.formatMessageWithCallsign(
                                                message,
                                                callsign,
                                                callsignEnabled
                                        )

                                // Handle QR Code separately
                                if (selectedMethod == TransmissionMethod.QR_CODE) {
                                        // Generate QR code
                                        val bitmap =
                                                qrGenerator.generateOptimalQRCode(formattedMessage)
                                        if (bitmap != null) {
                                                qrBitmap = bitmap
                                                showQRTransmission = true
                                        }
                                        return@launch
                                }

                                // For other methods, use encoding
                                isTransmitting = true
                                pulseCount = 0
                                totalPulses = 0

                                // Mostrar mensaje codificado
                                encodedMessage =
                                        when (selectedEncoding) {
                                                EncodingType.MORSE ->
                                                        morseEncoder.encodeToString(
                                                                formattedMessage
                                                        )
                                                EncodingType.ASCII_BINARY ->
                                                        binaryEncoder.encodeToString(
                                                                formattedMessage
                                                        )
                                        }

                                val timings =
                                        when (selectedEncoding) {
                                                EncodingType.MORSE ->
                                                        morseEncoder.encode(formattedMessage)
                                                EncodingType.ASCII_BINARY ->
                                                        binaryEncoder.encode(formattedMessage)
                                        }
                                when (selectedMethod) {
                                        TransmissionMethod.FLASHLIGHT ->
                                                flashlightController.transmit(timings)
                                        TransmissionMethod.VIBRATION ->
                                                vibrationController.transmit(timings)
                                        TransmissionMethod.SOUND ->
                                                soundController.transmit(timings)
                                        TransmissionMethod.QR_CODE -> {
                                                // Already handled above
                                        }
                                }
                                isTransmitting = false
                        }
                }
        }

        Column(
                modifier =
                        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
        ) {
                Text(
                        text = "Transmitir Mensaje",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Panel de debugging visual
                if (isTransmitting || encodedMessage.isNotEmpty()) {
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        if (flashlightState)
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                        else
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                        )
                        ) {
                                Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = "🔍 DEBUG",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )

                                                // Indicador visual de estado
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp)
                                                ) {
                                                        Text(
                                                                text =
                                                                        if (flashlightState) "💡 ON"
                                                                        else "⚫ OFF",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                                color =
                                                                        if (flashlightState)
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                        )
                                                }
                                        }

                                        if (totalPulses > 0) {
                                                LinearProgressIndicator(
                                                        progress =
                                                                pulseCount.toFloat() /
                                                                        totalPulses.toFloat(),
                                                        modifier = Modifier.fillMaxWidth()
                                                )
                                                Text(
                                                        text = "Pulso: $pulseCount / $totalPulses",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }

                                        if (encodedMessage.isNotEmpty()) {
                                                Divider(
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                                Text(
                                                        text = "Mensaje codificado:",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = encodedMessage,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.fillMaxWidth()
                                                )
                                        }
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                colors =
                                        ButtonDefaults.filledTonalButtonColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.errorContainer
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
                                colors =
                                        ButtonDefaults.buttonColors(
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

                // Tipo de codificación
                Text(
                        text = "Tipo de Codificación",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        FilterChip(
                                selected = selectedEncoding == EncodingType.MORSE,
                                onClick = { selectedEncoding = EncodingType.MORSE },
                                label = { Text("📻 Código Morse") },
                                enabled = !isTransmitting,
                                modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                                selected = selectedEncoding == EncodingType.ASCII_BINARY,
                                onClick = { selectedEncoding = EncodingType.ASCII_BINARY },
                                label = { Text("💻 ASCII Binario") },
                                enabled = !isTransmitting,
                                modifier = Modifier.weight(1f)
                        )
                }

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

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        FilterChip(
                                selected = selectedMethod == TransmissionMethod.SOUND,
                                onClick = { selectedMethod = TransmissionMethod.SOUND },
                                label = { Text("🔊 Ultrasonido") },
                                enabled = !isTransmitting,
                                modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                                selected = selectedMethod == TransmissionMethod.QR_CODE,
                                onClick = { selectedMethod = TransmissionMethod.QR_CODE },
                                label = { Text("📱 QR Code") },
                                enabled = !isTransmitting,
                                modifier = Modifier.weight(1f)
                        )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de enviar una sola vez
                Button(
                        onClick = { sendOnce(text) },
                        enabled = !isTransmitting && text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Enviar Una Vez", style = MaterialTheme.typography.titleMedium) }

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

        // QR Transmission overlay
        if (showQRTransmission && qrBitmap != null) {
                var displayMessage by remember { mutableStateOf(text) }

                LaunchedEffect(text, callsign, callsignEnabled) {
                        displayMessage =
                                userPreferences.formatMessageWithCallsign(
                                        text,
                                        callsign,
                                        callsignEnabled
                                )
                }

                QRTransmissionScreen(
                        qrBitmap = qrBitmap!!,
                        message = displayMessage,
                        flashlightController = flashlightController,
                        onDismiss = {
                                showQRTransmission = false
                                qrBitmap = null
                        }
                )
        }
}
