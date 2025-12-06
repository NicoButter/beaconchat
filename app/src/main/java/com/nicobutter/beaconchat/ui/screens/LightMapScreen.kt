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
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nicobutter.beaconchat.lightmap.DetectedDevice
import com.nicobutter.beaconchat.lightmap.HeartbeatPattern
import com.nicobutter.beaconchat.lightmap.LightScanner
import com.nicobutter.beaconchat.transceiver.FlashlightController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Light map screen for visualizing and managing detected light signals.
 *
 * Provides a radar-like interface for detecting and tracking devices using
 * light signals. Includes camera-based scanning, device visualization,
 * heartbeat transmission, and signal statistics display.
 *
 * @param flashlightController Controller for flashlight-based signaling
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun LightMapScreen(
    flashlightController: FlashlightController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    var isHeartbeatActive by remember { mutableStateOf(false) }
    var isScanningActive by remember { mutableStateOf(false) }
    
    val lightScanner = remember { LightScanner() }
    val detectedDevices by lightScanner.detectedDevices.collectAsState()
    val signalData by lightScanner.signalData.collectAsState()
    val signalStats by lightScanner.signalStats.collectAsState()
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Heartbeat automático
    LaunchedEffect(isHeartbeatActive) {
        if (isHeartbeatActive) {
            while (isActive) {
                try {
                    val heartbeat = HeartbeatPattern.generateHeartbeat()
                    flashlightController.transmit(heartbeat)
                    delay(HeartbeatPattern.HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    // Si hay error, detener el heartbeat
                    isHeartbeatActive = false
                    break
                }
            }
        } else {
            // Asegurar que la linterna se apague cuando se desactiva el heartbeat
            flashlightController.stop()
        }
    }
    
    // Limpiar recursos al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            // Detener heartbeat y limpiar flash
            isHeartbeatActive = false
            isScanningActive = false
            flashlightController.stop()
            // Dar tiempo para que se libere el mutex antes de cleanup
            Thread.sleep(50)
            flashlightController.cleanup()
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "LightMap Radar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Detección visual de dispositivos cercanos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Controles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Heartbeat
                    FilterChip(
                        selected = isHeartbeatActive,
                        onClick = { isHeartbeatActive = !isHeartbeatActive },
                        label = {
                            Text(if (isHeartbeatActive) "Heartbeat ON" else "Heartbeat OFF")
                        },
                        leadingIcon = {
                            if (isHeartbeatActive) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Botón Scan
                    FilterChip(
                        selected = isScanningActive,
                        onClick = { isScanningActive = !isScanningActive },
                        label = {
                            Text(if (isScanningActive) "Scanning..." else "Start Scan")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Radar visual
        if (isScanningActive && hasCameraPermission) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Preview de cámara (oculto, solo para análisis)
                CameraPreview(
                    lightScanner = lightScanner,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.size(1.dp) // Oculto pero activo
                )
                
                // Radar overlay con tamaño controlado
                RadarOverlay(
                    devices = detectedDevices,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Mantener cuadrado
                        .align(Alignment.Center)
                )
            }
        } else {
            // Instrucciones
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (!hasCameraPermission) {
                            "Permiso de cámara requerido"
                        } else {
                            "Activa el Heartbeat y el Scanner para detectar otros dispositivos"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Mini-osciloscopio de señal (debajo del radar)
        if (isScanningActive && hasCameraPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF001100)
                )
            ) {
                MiniOscilloscope(
                    signalData = signalData,
                    stats = signalStats,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Lista de dispositivos detectados
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Dispositivos detectados (${detectedDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (detectedDevices.isEmpty()) {
                    Text(
                        "No hay dispositivos detectados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedDevices) { device ->
                            DeviceCard(device = device)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    lightScanner: LightScanner,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Liberar cámara cuando se desmonta el composable
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Configuración optimizada para máxima sensibilidad a flashes
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(640, 480)) // Resolución baja para mejor performance
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setImageQueueDepth(1) // Solo mantener 1 frame en cola
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor(), lightScanner)
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    
                    // Configurar cámara para máxima sensibilidad a flashes
                    camera.cameraControl.setExposureCompensationIndex(
                        camera.cameraInfo.exposureState.exposureCompensationRange.upper
                    )
                    
                    // Configurar foco infinito para mejor detección de flashes distantes
                    camera.cameraControl.setLinearZoom(0f)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}

@Composable
private fun RadarOverlay(
    devices: List<DetectedDevice>,
    modifier: Modifier = Modifier
) {
    // Animación de escaneo más lenta (4 segundos en lugar de 3)
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )
    
    Canvas(modifier = modifier.background(Color.Black.copy(alpha = 0.7f))) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = minOf(size.width, size.height) / 2 * 0.8f
        
        // Solo 2 círculos concéntricos (antes eran 3)
        for (i in 1..2) {
            drawCircle(
                color = Color.Green.copy(alpha = 0.3f),
                radius = maxRadius * i / 2,
                center = center,
                style = Stroke(width = 2f)
            )
        }
        
        // Línea de escaneo giratoria
        val scanAngleRad = Math.toRadians(radarAngle.toDouble())
        val scanEnd = Offset(
            center.x + (maxRadius * cos(scanAngleRad)).toFloat(),
            center.y + (maxRadius * sin(scanAngleRad)).toFloat()
        )
        drawLine(
            color = Color.Green.copy(alpha = 0.5f),
            start = center,
            end = scanEnd,
            strokeWidth = 3f
        )
        
        // Puntos de dispositivos detectados (máximo 5 para no saturar)
        devices.take(5).forEach { device ->
            val angleRad = Math.toRadians(device.angle.toDouble())
            val distanceRatio = (10 - device.estimatedDistance) / 10 // Normalizar
            val pointRadius = maxRadius * distanceRatio.coerceIn(0.2f, 0.9f)
            
            val point = Offset(
                center.x + (pointRadius * cos(angleRad)).toFloat(),
                center.y + (pointRadius * sin(angleRad)).toFloat()
            )
            
            // Punto del dispositivo
            drawCircle(
                color = when {
                    device.intensity > 180 -> Color.Red
                    device.intensity > 120 -> Color.Yellow
                    else -> Color.Green
                },
                radius = 10f,
                center = point
            )
        }
        
        // Centro (este dispositivo)
        drawCircle(
            color = Color.Cyan,
            radius = 8f,
            center = center
        )
    }
}

@Composable
private fun MiniOscilloscope(
    signalData: List<LightScanner.IntensityPoint>,
    stats: LightScanner.SignalStats,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val width = size.width
            val height = size.height
            
            // Línea central (referencia)
            drawLine(
                color = Color.Green.copy(alpha = 0.2f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 1f
            )
            
            if (signalData.isEmpty()) return@Canvas
            
            // Graficar señal
            val pointSpacing = width / signalData.size.coerceAtLeast(1)
            
            signalData.forEachIndexed { index, point ->
                val x = index * pointSpacing
                val y = height - (point.intensity.toFloat() / 255f * height)
                
                // Línea vertical para cada punto
                if (index > 0) {
                    val prevPoint = signalData[index - 1]
                    val prevX = (index - 1) * pointSpacing
                    val prevY = height - (prevPoint.intensity.toFloat() / 255f * height)
                    
                    drawLine(
                        color = Color.Green,
                        start = Offset(prevX, prevY),
                        end = Offset(x, y),
                        strokeWidth = 1.5f
                    )
                }
                
                // Marcar picos en rojo
                if (point.isPeak) {
                    drawCircle(
                        color = Color.Red,
                        radius = 3f,
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        // Stats overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Text(
                "${stats.fps} FPS",
                color = Color.Green,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                "Noise: ${stats.noiseLevel}",
                color = Color.Yellow,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DeviceCard(device: DetectedDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Dispositivo ${device.id.takeLast(4)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    device.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (device.getSignalQuality()) {
                                "Excelente" -> Color.Green
                                "Buena" -> Color.Yellow
                                "Regular" -> Color(0xFFFFA500)
                                else -> Color.Red
                            }
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    device.timeSinceLastSeen(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
