package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.transceiver.VibrationOscilloscope

/**
 * Pantalla del Detector de Vibración
 * 
 * Muestra un osciloscopio en tiempo real de las vibraciones captadas
 * por el acelerómetro y decodifica mensajes Morse transmitidos táctilmente.
 * 
 * Uso:
 * 1. Colocar ambos dispositivos en contacto directo (superficie contra superficie)
 * 2. Presionar "Iniciar Detección"
 * 3. El dispositivo transmisor vibra el mensaje en Morse
 * 4. Esta pantalla muestra el patrón de vibración y decodifica el mensaje
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationDetectorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val oscilloscope = remember { VibrationOscilloscope(context) }
    
    // Estado de la UI
    var isAnalyzing by remember { mutableStateOf(false) }
    var currentMagnitude by remember { mutableStateOf(0f) }
    var decodedMessage by remember { mutableStateOf("") }
    var detectedPulses by remember { mutableStateOf(0) }
    var magnitudeHistory by remember { mutableStateOf(listOf<Float>()) }
    var stats by remember { mutableStateOf(oscilloscope.getStats()) }
    
    // Actualizar estado desde el osciloscopio
    LaunchedEffect(oscilloscope.isAnalyzing) {
        isAnalyzing = oscilloscope.isAnalyzing
        
        while (oscilloscope.isAnalyzing) {
            currentMagnitude = oscilloscope.currentMagnitude
            decodedMessage = oscilloscope.decodedMessage
            detectedPulses = oscilloscope.detectedPulses
            magnitudeHistory = oscilloscope.getMagnitudeHistory()
            stats = oscilloscope.getStats()
            
            kotlinx.coroutines.delay(50) // Actualizar 20 veces por segundo
        }
    }
    
    // Limpiar recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            oscilloscope.stopAnalysis()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detector de Vibración") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Refresh, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instrucciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "📳 Detector Táctil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Coloca este dispositivo en contacto directo con el transmisor\n" +
                        "2. Presiona 'Iniciar Detección'\n" +
                        "3. El transmisor debe vibrar su mensaje en Morse\n" +
                        "4. El mensaje aparecerá decodificado abajo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (isAnalyzing) {
                            oscilloscope.stopAnalysis()
                        } else {
                            oscilloscope.startAnalysis()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAnalyzing) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isAnalyzing) Icons.Default.Clear else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isAnalyzing) "Detener" else "Iniciar Detección")
                }
                
                OutlinedButton(
                    onClick = { oscilloscope.reset() },
                    enabled = !isAnalyzing
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Limpiar")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estado de detección
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAnalyzing) 
                        MaterialTheme.colorScheme.secondaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAnalyzing) "🔴 DETECTANDO..." else "⚪ DETENIDO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Osciloscopio de vibración
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "Intensidad de Vibración",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Gráfico de osciloscopio
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val width = size.width
                        val height = size.height
                        
                        // Línea base (0%)
                        drawLine(
                            color = Color.Gray,
                            start = Offset(0f, height),
                            end = Offset(width, height),
                            strokeWidth = 1f
                        )
                        
                        // Umbral dinámico (40%)
                        val thresholdY = height * 0.6f
                        drawLine(
                            color = Color.Yellow.copy(alpha = 0.5f),
                            start = Offset(0f, thresholdY),
                            end = Offset(width, thresholdY),
                            strokeWidth = 2f
                        )
                        
                        // Gráfico de magnitud de vibración
                        if (magnitudeHistory.size > 1) {
                            val path = Path()
                            val stepX = width / magnitudeHistory.size
                            
                            path.moveTo(0f, height - (magnitudeHistory[0] * height))
                            
                            magnitudeHistory.forEachIndexed { index, magnitude ->
                                val x = index * stepX
                                val y = height - (magnitude * height) // Invertir Y
                                path.lineTo(x, y)
                            }
                            
                            drawPath(
                                path = path,
                                color = Color(0xFF00FF00), // Verde brillante
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Estadísticas de señal
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Estadísticas de Señal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Mín:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "%.2f m/s²".format(stats.min),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Máx:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "%.2f m/s²".format(stats.max),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Promedio:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "%.2f m/s²".format(stats.avg),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Actual:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "%.2f m/s²".format(stats.current),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pulsos detectados:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$detectedPulses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Magnitud actual:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${(currentMagnitude * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (currentMagnitude > 0.4f) 
                                    Color(0xFF00FF00) 
                                else 
                                    Color.Gray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje decodificado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Mensaje Decodificado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = decodedMessage.ifEmpty { "(esperando mensaje...)" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (decodedMessage.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color.Gray
                    )
                }
            }
        }
    }
}
