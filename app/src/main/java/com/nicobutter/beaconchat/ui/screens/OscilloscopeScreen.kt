package com.nicobutter.beaconchat.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.nicobutter.beaconchat.lightmap.OpticalOscilloscope
import java.util.concurrent.Executors

/**
 * Optical oscilloscope screen for real-time light signal analysis.
 *
 * Provides a visual interface for monitoring light intensity changes and
 * decoding Morse code signals. Displays waveform visualization, signal
 * statistics, and decoded messages in real-time using camera input.
 *
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun OscilloscopeScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Crear osciloscopio
    val oscilloscope = remember { OpticalOscilloscope() }
    
    // Observar datos de señal
    val signalData by oscilloscope.signalData.collectAsState()
    val decodedMessage by oscilloscope.decodedMessage.collectAsState()
    val stats by oscilloscope.signalStats.collectAsState()
    
    // Iniciar escaneo automáticamente al entrar
    LaunchedEffect(Unit) {
        // El escaneo se inicia automáticamente cuando se crea la cámara
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header con estadísticas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "OPTICAL OSCILLOSCOPE",
                    color = Color.Green,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "${stats.fps} FPS | Range: ${stats.minIntensity}-${stats.maxIntensity}",
                    color = Color.Green.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Protocolo: DOT<200ms DASH:200-500ms",
                    color = Color.Cyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Filtro: Suavizado 0.7x",
                    color = Color.Yellow.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Indicador de escaneo activo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.Green, shape = MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SCANNING",
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Gráfico del osciloscopio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF001100)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Gráfico de señal
                SignalGraph(
                    signalData = signalData,
                    stats = stats,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay con info
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        "Min: ${stats.minIntensity}",
                        color = Color.Cyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Avg: ${stats.avgIntensity}",
                        color = Color.Green,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Max: ${stats.maxIntensity}",
                        color = Color.Yellow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mensaje decodificado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF002200)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "DECODED MESSAGE:",
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    decodedMessage.ifEmpty { "Waiting for signal..." },
                    color = if (decodedMessage.isEmpty()) Color.Green.copy(alpha = 0.5f) else Color.Green,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Vista previa de cámara (pequeña, para referencia)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            )
        ) {
            CameraPreview(
                oscilloscope = oscilloscope,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Instrucciones
        Text(
            "📡 Point camera at flashing light source\n" +
            "🔍 The oscilloscope will detect and decode Morse signals\n" +
            "⚡ Works in dark, dusty, or poor visibility environments",
            color = Color.Green.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SignalGraph(
    signalData: List<OpticalOscilloscope.IntensityPoint>,
    stats: OpticalOscilloscope.SignalStats,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        
        // Ejes
        drawLine(
            color = Color.Green.copy(alpha = 0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f
        )
        
        // Líneas de referencia (niveles)
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = Color.Green.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (signalData.isEmpty()) return@Canvas
        
        // Graficar señal
        val path = Path()
        val pointSpacing = width / signalData.size.coerceAtLeast(1)
        
        signalData.forEachIndexed { index, point ->
            val x = index * pointSpacing
            // Normalizar intensidad (0-255) a altura del canvas
            val y = height - (point.intensity.toFloat() / 255f * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            
            // Marcar picos y valles
            if (point.isPeak) {
                drawCircle(
                    color = Color.Red,
                    radius = 4f,
                    center = Offset(x, y)
                )
            } else if (point.isValley) {
                drawCircle(
                    color = Color.Cyan,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
        
        // Dibujar la curva de señal
        drawPath(
            path = path,
            color = Color.Green,
            style = Stroke(width = 2f)
        )
        
        // Línea del umbral promedio
        val avgY = height - (stats.avgIntensity.toFloat() / 255f * height)
        drawLine(
            color = Color.Yellow.copy(alpha = 0.5f),
            start = Offset(0f, avgY),
            end = Offset(width, avgY),
            strokeWidth = 1f
        )
    }
}

@Composable
private fun CameraPreview(
    oscilloscope: OpticalOscilloscope,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Configuración estándar sin filtros
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor(), oscilloscope)
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier
    )
}
