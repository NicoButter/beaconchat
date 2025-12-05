package com.nicobutter.beaconchat.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.transceiver.FlashlightController
import kotlinx.coroutines.delay

/**
 * QR code transmission screen for displaying scannable QR codes.
 *
 * Shows a QR code bitmap with automatic dismissal after a timeout period.
 * Includes attention-grabbing flashlight flashes and countdown display
 * to guide the scanning process.
 *
 * @param qrBitmap The QR code bitmap to display
 * @param message The original message encoded in the QR code
 * @param flashlightController Controller for attention-grabbing flashes
 * @param onDismiss Callback invoked when the screen should be dismissed
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun QRTransmissionScreen(
        qrBitmap: Bitmap,
        message: String,
        flashlightController: FlashlightController,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isFlashing by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(10) }

    // Auto-dismiss after 10 seconds
    LaunchedEffect(Unit) {
        // Flash pattern: 3 quick flashes to attract attention
        repeat(3) {
            flashlightController.transmit(listOf(100L, 100L)) // ON 100ms, OFF 100ms
            delay(200)
        }

        // Countdown
        for (i in 10 downTo 1) {
            timeRemaining = i
            delay(1000)
        }

        onDismiss()
    }

    Box(
            modifier = modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
        ) {
            // QR Code
            Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code: $message",
                    modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Message
            Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer
            Text(
                    text = "Auto-cierre en ${timeRemaining}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
            )
        }

        // Close button
        FloatingActionButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error
        ) {
            Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
            )
        }

        // Instructions at bottom
        Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
        ) {
            Text(
                    text = "📷 Apunta la cámara del receptor hacia este código QR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
            )
        }
    }
}
