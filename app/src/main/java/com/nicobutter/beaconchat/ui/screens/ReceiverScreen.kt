package com.nicobutter.beaconchat.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nicobutter.beaconchat.transceiver.LightDetector
import com.nicobutter.beaconchat.transceiver.MorseDecoder
import com.nicobutter.beaconchat.transceiver.QRScanner
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Receiver screen for detecting and decoding incoming messages.
 *
 * Provides camera-based detection for both light signals (Morse code) and QR codes,
 * plus vibration detection via accelerometer.
 * Handles camera permissions, switches between detection modes, and displays
 * decoded messages in real-time.
 *
 * @param modifier Modifier for customizing the layout
 * @param lifecycleOwner Lifecycle owner for camera management
 * @param onNavigateToVibrationDetector Callback to navigate to vibration detector screen
 */
@Composable
fun ReceiverScreen(
        modifier: Modifier = Modifier,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        onNavigateToVibrationDetector: () -> Unit = {}
) {
        val context = LocalContext.current
        var isLightOn by remember { mutableStateOf(false) }
        var isQrMode by remember { mutableStateOf(false) }
        var qrMessage by remember { mutableStateOf("") }
        var lastQrDetectionTime by remember { mutableStateOf(0L) }

        val morseDecoder = remember { MorseDecoder() }
        val previewView = remember { PreviewView(context) }
        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

        var hasCameraPermission by remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                )
        }

        val launcher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { granted -> hasCameraPermission = granted }
                )

        LaunchedEffect(Unit) {
                if (!hasCameraPermission) {
                        launcher.launch(Manifest.permission.CAMERA)
                }
        }

        // Camera setup
        LaunchedEffect(hasCameraPermission, isQrMode) {
                if (hasCameraPermission) {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).await()

                        val preview =
                                Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                        val imageAnalysis =
                                ImageAnalysis.Builder()
                                        .setBackpressureStrategy(
                                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                        )
                                        .build()

                        if (isQrMode) {
                                imageAnalysis.setAnalyzer(
                                        analysisExecutor,
                                        QRScanner { content ->
                                                qrMessage = content
                                                lastQrDetectionTime = System.currentTimeMillis()
                                        }
                                )
                        } else {
                                imageAnalysis.setAnalyzer(
                                        analysisExecutor,
                                        LightDetector { lightDetected ->
                                                isLightOn = lightDetected
                                                morseDecoder.onLightStateChanged(lightDetected)
                                        }
                                )
                        }

                        try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                )
                        } catch (exc: Exception) {
                                exc.printStackTrace()
                        }
                }
        }

        if (hasCameraPermission) {
                Box(modifier = modifier.fillMaxSize()) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                        // Mode Toggle
                        Row(
                                modifier =
                                        Modifier.align(Alignment.TopCenter)
                                                .padding(top = 16.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.8f
                                                        ),
                                                        MaterialTheme.shapes.extraLarge
                                                )
                                                .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                                FilterChip(
                                        selected = !isQrMode,
                                        onClick = { isQrMode = false },
                                        label = { Text("Luz (Morse)") },
                                        leadingIcon = { Icon(Icons.Default.Info, null) }
                                )
                                FilterChip(
                                        selected = isQrMode,
                                        onClick = { isQrMode = true },
                                        label = { Text("QR Code") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) }
                                )
                                FilterChip(
                                        selected = false,
                                        onClick = onNavigateToVibrationDetector,
                                        label = { Text("Vibración") },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                        }

                        // Info Panel
                        Column(
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(16.dp)
                                                .fillMaxWidth()
                                                .background(
                                                        MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.9f
                                                        ),
                                                        MaterialTheme.shapes.medium
                                                )
                                                .padding(16.dp)
                        ) {
                                if (isQrMode) {
                                        Text(
                                                text = "Escáner QR Activo",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (qrMessage.isNotEmpty()) {
                                                Text(
                                                        text = "Último mensaje recibido:",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Text(
                                                        text = qrMessage,
                                                        style =
                                                                MaterialTheme.typography
                                                                        .headlineSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Button(
                                                        onClick = { qrMessage = "" },
                                                        modifier = Modifier.align(Alignment.End)
                                                ) { Text("Limpiar") }
                                        } else {
                                                Text(
                                                        text =
                                                                "Apunta a un código QR para escanear...",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                } else {
                                        Text(
                                                text =
                                                        "Status: ${if (isLightOn) "LIGHT ON" else "LIGHT OFF"}",
                                                color = if (isLightOn) Color.Green else Color.Red,
                                                style = MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text =
                                                        if (morseDecoder.decodedMessage.isEmpty())
                                                                "Esperando señal..."
                                                        else morseDecoder.decodedMessage,
                                                style = MaterialTheme.typography.bodyLarge
                                        )
                                        Button(onClick = { morseDecoder.reset() }) {
                                                Text("Limpiar")
                                        }
                                }
                        }
                }
        } else {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Se requiere permiso de cámara para recibir mensajes.")
                }
        }
}

// Extension to await ProcessCameraProvider
private suspend fun com.google.common.util.concurrent.ListenableFuture<
        ProcessCameraProvider>.await(): ProcessCameraProvider {
        return suspendCoroutine { cont ->
                addListener(
                        {
                                try {
                                        cont.resume(get())
                                } catch (e: Exception) {
                                        cont.resumeWith(Result.failure(e))
                                }
                        },
                        { command -> command.run() }
                ) // Run directly or use main executor
        }
}
