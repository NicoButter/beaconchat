package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.nicobutter.beaconchat.transceiver.LightDetector
import com.nicobutter.beaconchat.transceiver.MorseDecoder

@Composable
fun ReceiverScreen(modifier: Modifier = Modifier, lifecycleOwner: LifecycleOwner = androidx.compose.ui.platform.LocalContext.current as LifecycleOwner) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var isLightOn by remember { mutableStateOf(false) }

        val morseDecoder = remember { MorseDecoder() }

        val cameraProviderFuture = remember {
                androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        }

        var hasCameraPermission by remember {
                mutableStateOf(
                        androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )
        }

        val launcher =
                androidx.activity.compose.rememberLauncherForActivityResult(
                        contract =
                                androidx.activity.result.contract.ActivityResultContracts
                                        .RequestPermission(),
                        onResult = { granted -> hasCameraPermission = granted }
                )

        LaunchedEffect(Unit) {
                if (!hasCameraPermission) {
                        launcher.launch(android.Manifest.permission.CAMERA)
                }
        }

        if (hasCameraPermission) {
                Box(modifier = modifier.fillMaxSize()) {
                        androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                        val previewView = androidx.camera.view.PreviewView(ctx)
                                        // Use a background executor for image analysis to avoid
                                        // blocking the main
                                        // thread
                                        val analysisExecutor =
                                                java.util.concurrent.Executors
                                                        .newSingleThreadExecutor()

                                        cameraProviderFuture.addListener(
                                                {
                                                        val cameraProvider =
                                                                cameraProviderFuture.get()
                                                        val preview =
                                                                androidx.camera.core.Preview
                                                                        .Builder()
                                                                        .build()
                                                                        .also {
                                                                                it.setSurfaceProvider(
                                                                                        previewView
                                                                                                .surfaceProvider
                                                                                )
                                                                        }

                                                        val imageAnalysis =
                                                                androidx.camera.core.ImageAnalysis
                                                                        .Builder()
                                                                        .setBackpressureStrategy(
                                                                                androidx.camera.core
                                                                                        .ImageAnalysis
                                                                                        .STRATEGY_KEEP_ONLY_LATEST
                                                                        )
                                                                        .build()
                                                                        .also {
                                                                                it.setAnalyzer(
                                                                                        analysisExecutor,
                                                                                        LightDetector {
                                                                                                lightDetected
                                                                                                ->
                                                                                                isLightOn =
                                                                                                        lightDetected
                                                                                                morseDecoder
                                                                                                        .onLightStateChanged(
                                                                                                                lightDetected
                                                                                                        )
                                                                                        }
                                                                                )
                                                                        }

                                                        try {
                                                                cameraProvider.unbindAll()
                                                                cameraProvider.bindToLifecycle(
                                                                        lifecycleOwner,
                                                                        androidx.camera.core
                                                                                .CameraSelector
                                                                                .DEFAULT_BACK_CAMERA,
                                                                        preview,
                                                                        imageAnalysis
                                                                )
                                                        } catch (exc: Exception) {
                                                                exc.printStackTrace()
                                                        }
                                                },
                                                androidx.core.content.ContextCompat.getMainExecutor(
                                                        ctx
                                                )
                                        )

                                        previewView
                                },
                                modifier = Modifier.fillMaxSize()
                        )

                        Column(
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(16.dp)
                                                .fillMaxWidth()
                                                .background(
                                                        MaterialTheme.colorScheme.surface.copy(
                                                                alpha = 0.7f
                                                        ),
                                                        MaterialTheme.shapes.medium
                                                )
                                                .padding(16.dp)
                        ) {
                                Text(
                                        text =
                                                "Status: ${if (isLightOn) "LIGHT ON" else "LIGHT OFF"}",
                                        color =
                                                if (isLightOn)
                                                        androidx.compose.ui.graphics.Color.Green
                                                else androidx.compose.ui.graphics.Color.Red,
                                        style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text =
                                                if (morseDecoder.decodedMessage.isEmpty())
                                                        "Waiting for signal..."
                                                else morseDecoder.decodedMessage,
                                        style = MaterialTheme.typography.bodyLarge
                                )
                                Button(onClick = { morseDecoder.reset() }) { Text("Clear") }
                        }
                }
        } else {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Camera permission required to receive messages.")
                }
        }
}
