package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicobutter.beaconchat.scanner.BleScanner
import com.nicobutter.beaconchat.scanner.DetectedEmergency
import com.nicobutter.beaconchat.transceiver.LightDetector
import com.nicobutter.beaconchat.transceiver.MorseDecoder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nicobutter.beaconchat.domain.EmergencyType

/**
 * "Buscar señales" — unified signal scanner screen.
 *
 * Combines two reception channels in one view:
 * - **BLE channel**: scans for BeaconChat emergency advertisements via [BleScanner].
 * - **Optical channel**: camera + [LightDetector] + [MorseDecoder] for flashlight Morse.
 *
 * Both channels run in parallel while this screen is visible.
 */
@Composable
fun SignalScannerScreen(
    bleScanner: BleScanner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detectedEmergencies by bleScanner.detectedEmergencies.collectAsState()
    val isScanning by bleScanner.isScanning.collectAsState()

    val morseDecoder = remember { MorseDecoder() }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasCameraPermission = it }
    )

    // Start BLE scanning when screen is shown, stop on leave
    DisposableEffect(Unit) {
        bleScanner.start()
        onDispose { bleScanner.stop() }
    }

    // Bind camera when permission granted
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return@LaunchedEffect
        }
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val detector = LightDetector { isOn, durationMs ->
            morseDecoder.onLightStateChanged(isOn, durationMs)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, detector) }

        provider.unbindAll()
        try {
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Buscar Señales",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "BLE + Óptico simultáneo",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            ScanIndicator(isScanning = isScanning)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Optical channel ──────────────────────────────────────────────────
        SectionCard(title = "📷 Canal Óptico", color = Color(0xFF1A237E)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                val decoded = morseDecoder.decodedMessage
                if (decoded.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Recibido: $decoded",
                        color = Color(0xFF80CBC4),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Esperando señal de linterna…",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Text(
                    "Permiso de cámara requerido",
                    color = Color(0xFFEF9A9A),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── BLE channel ───────────────────────────────────────────────────────
        SectionCard(title = "📡 Canal BLE") {
            if (detectedEmergencies.isEmpty()) {
                Text(
                    "Sin beacons de emergencia detectados",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detectedEmergencies) { emergency ->
                        EmergencyBeaconCard(emergency)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanIndicator(isScanning: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (isScanning) Color(0xFF69F0AE) else Color(0xFF616161),
                    shape = RoundedCornerShape(50)
                )
        )
        Text(
            if (isScanning) "Escaneando" else "Inactivo",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    color: Color = Color(0xFF1C1C1E),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EmergencyBeaconCard(emergency: DetectedEmergency) {
    val typeColor = Color(emergency.emergencyType.colorArgb)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emergency.emergencyType.icon, fontSize = 28.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                emergency.emergencyType.displayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                emergency.deviceAddress,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${emergency.rssi} dBm",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                emergency.signalQuality(),
                color = typeColor,
                fontSize = 11.sp
            )
        }
    }
}
