package com.nicobutter.beaconchat.lightmap

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

/**
 * OpticalOscilloscope - Convierte la cámara en un osciloscopio óptico
 * 
 * Lee intensidad de luz en tiempo real y genera una curva de señal
 * que puede decodificar Morse, Binary, y otros patrones con precisión quirúrgica.
 */
class OpticalOscilloscope : ImageAnalysis.Analyzer {
    
    // Buffer circular de intensidades (últimos N frames)
    private val intensityBuffer = ArrayDeque<IntensityPoint>(BUFFER_SIZE)
    
    // StateFlow para la UI
    private val _signalData = MutableStateFlow<List<IntensityPoint>>(emptyList())
    val signalData: StateFlow<List<IntensityPoint>> = _signalData.asStateFlow()
    
    // Detección de mensajes decodificados
    private val _decodedMessage = MutableStateFlow<String>("")
    val decodedMessage: StateFlow<String> = _decodedMessage.asStateFlow()
    
    // Estadísticas de señal
    private val _signalStats = MutableStateFlow(SignalStats())
    val signalStats: StateFlow<SignalStats> = _signalStats.asStateFlow()
    
    // Detector de pulsos (para Morse)
    private val pulseDetector = PulseDetector()
    
    data class IntensityPoint(
        val timestamp: Long,
        val intensity: Int, // 0-255
        val isPeak: Boolean = false,
        val isValley: Boolean = false
    )
    
    data class SignalStats(
        val fps: Int = 0,
        val avgIntensity: Int = 0,
        val minIntensity: Int = 255,
        val maxIntensity: Int = 0,
        val noiseLevel: Int = 0
    )
    
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var lastFpsUpdate = 0L
    
    override fun analyze(image: ImageProxy) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Extraer intensidad promedio del frame (ROI centrado como LightDetector)
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)
            val intensity = calculateCenterIntensity(image, data)
            
            // Crear punto de intensidad
            val point = IntensityPoint(
                timestamp = currentTime,
                intensity = intensity
            )
            
            // Agregar al buffer circular
            intensityBuffer.addLast(point)
            if (intensityBuffer.size > BUFFER_SIZE) {
                intensityBuffer.removeFirst()
            }
            
            // Detectar picos y valles (para visualización y análisis)
            if (intensityBuffer.size >= 5) {
                markPeaksAndValleys()
            }
            
            // Actualizar StateFlow para la UI
            _signalData.value = intensityBuffer.toList()
            
            // Detector de pulsos Morse
            if (intensityBuffer.size >= 10) {
                val detectedPulses = pulseDetector.detectPulses(intensityBuffer.toList())
                if (detectedPulses.isNotEmpty()) {
                    val message = pulseDetector.decodeMorse(detectedPulses)
                    if (message.isNotEmpty()) {
                        _decodedMessage.value = message
                        Log.d(TAG, "Decoded Morse: $message")
                    }
                }
            }
            
            // Calcular estadísticas
            updateStats(currentTime)
            
            lastFrameTime = currentTime
            frameCount++
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame", e)
        } finally {
            image.close()
        }
    }
    
    private fun calculateCenterIntensity(image: ImageProxy, data: ByteArray): Int {
        // Usar la misma técnica que LightDetector para mejor detección
        val width = image.width
        val height = image.height
        val centerX = width / 2
        val centerY = height / 2
        val cropSize = minOf(width, height) / 6

        var sum = 0L
        var count = 0

        val startX = maxOf(0, centerX - cropSize / 2)
        val startY = maxOf(0, centerY - cropSize / 2)
        val endX = minOf(width, centerX + cropSize / 2)
        val endY = minOf(height, centerY + cropSize / 2)

        val rowStride = image.planes[0].rowStride

        for (y in startY until endY) {
            for (x in startX until endX) {
                val index = y * rowStride + x
                if (index < data.size) {
                    sum += (data[index].toInt() and 0xFF)
                    count++
                }
            }
        }

        return if (count > 0) (sum / count).toInt().coerceIn(0, 255) else 0
    }
    
    private fun markPeaksAndValleys() {
        if (intensityBuffer.size < 5) return
        
        val recent = intensityBuffer.takeLast(5).toMutableList()
        val middle = recent[2]
        
        // Detectar pico (máximo local)
        val isPeak = middle.intensity > recent[1].intensity && 
                     middle.intensity > recent[3].intensity &&
                     middle.intensity > recent[0].intensity &&
                     middle.intensity > recent[4].intensity
        
        // Detectar valle (mínimo local)
        val isValley = middle.intensity < recent[1].intensity && 
                       middle.intensity < recent[3].intensity &&
                       middle.intensity < recent[0].intensity &&
                       middle.intensity < recent[4].intensity
        
        if (isPeak || isValley) {
            val index = intensityBuffer.size - 3
            intensityBuffer[index] = middle.copy(isPeak = isPeak, isValley = isValley)
        }
    }
    
    private fun updateStats(currentTime: Long) {
        if (intensityBuffer.isEmpty()) return
        
        // Calcular FPS cada segundo
        val fps = if (currentTime - lastFpsUpdate >= 1000) {
            val calculatedFps = frameCount
            frameCount = 0
            lastFpsUpdate = currentTime
            calculatedFps
        } else {
            _signalStats.value.fps
        }
        
        // Calcular estadísticas de señal
        val intensities = intensityBuffer.map { it.intensity }
        val avg = intensities.average().toInt()
        val min = intensities.minOrNull() ?: 0
        val max = intensities.maxOrNull() ?: 255
        
        // Calcular nivel de ruido (desviación estándar)
        val variance = intensities.map { (it - avg) * (it - avg) }.average()
        val noise = kotlin.math.sqrt(variance).toInt()
        
        _signalStats.value = SignalStats(
            fps = fps,
            avgIntensity = avg,
            minIntensity = min,
            maxIntensity = max,
            noiseLevel = noise
        )
    }
    
    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
    
    fun reset() {
        intensityBuffer.clear()
        _signalData.value = emptyList()
        _decodedMessage.value = ""
        _signalStats.value = SignalStats()
        pulseDetector.reset()
        frameCount = 0
        lastFpsUpdate = 0L
    }
    
    companion object {
        private const val TAG = "OpticalOscilloscope"
        private const val BUFFER_SIZE = 300 // ~10 segundos a 30 FPS
    }
}

/**
 * PulseDetector - Detecta pulsos Morse en la curva de señal
 */
class PulseDetector {
    private val pulseHistory = mutableListOf<Pulse>()
    private val intensityHistory = ArrayDeque<Int>()
    private val historySize = 30 // Últimos 30 frames para calcular umbral
    
    data class Pulse(
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val type: PulseType
    )
    
    enum class PulseType {
        DOT,    // Pulso corto
        DASH,   // Pulso largo
        GAP     // Silencio entre pulsos
    }
    
    fun detectPulses(signal: List<OpticalOscilloscope.IntensityPoint>): List<Pulse> {
        val pulses = mutableListOf<Pulse>()
        
        // Actualizar historial de intensidades para umbral dinámico
        signal.forEach { point ->
            intensityHistory.addLast(point.intensity)
            if (intensityHistory.size > historySize) {
                intensityHistory.removeFirst()
            }
        }
        
        // Calcular umbral dinámico basado en min/max como LightDetector
        val min = intensityHistory.minOrNull() ?: 0
        val max = intensityHistory.maxOrNull() ?: 255
        
        // Si hay suficiente rango dinámico, usar umbral en el medio
        // Si no, usar un umbral basado en promedio
        val threshold = if (max - min > 20) {
            (max + min) / 2
        } else {
            val avg = intensityHistory.average().toInt()
            avg + 10
        }
        
        var inPulse = false
        var pulseStart = 0L
        
        for (point in signal) {
            if (!inPulse && point.intensity > threshold) {
                // Inicio de pulso (ON)
                inPulse = true
                pulseStart = point.timestamp
            } else if (inPulse && point.intensity < threshold) {
                // Fin de pulso (OFF)
                inPulse = false
                val duration = point.timestamp - pulseStart
                
                if (duration > 30) { // Ignorar pulsos muy cortos (ruido)
                    val type = if (duration < 150) PulseType.DOT else PulseType.DASH
                    pulses.add(Pulse(pulseStart, point.timestamp, duration, type))
                }
            }
        }
        
        return pulses
    }
    
    fun decodeMorse(pulses: List<Pulse>): String {
        if (pulses.isEmpty()) return ""
        
        val morseCode = StringBuilder()
        
        for (pulse in pulses) {
            when (pulse.type) {
                PulseType.DOT -> morseCode.append(".")
                PulseType.DASH -> morseCode.append("-")
                PulseType.GAP -> morseCode.append(" ")
            }
        }
        
        return morseToText(morseCode.toString())
    }
    
    private fun morseToText(morse: String): String {
        val morseMap = mapOf(
            ".-" to "A", "-..." to "B", "-.-." to "C", "-.." to "D", "." to "E",
            "..-." to "F", "--." to "G", "...." to "H", ".." to "I", ".---" to "J",
            "-.-" to "K", ".-.." to "L", "--" to "M", "-." to "N", "---" to "O",
            ".--." to "P", "--.-" to "Q", ".-." to "R", "..." to "S", "-" to "T",
            "..-" to "U", "...-" to "V", ".--" to "W", "-..-" to "X", "-.--" to "Y",
            "--.." to "Z",
            "...---..." to "SOS"
        )
        
        return morse.split(" ")
            .mapNotNull { morseMap[it] }
            .joinToString("")
    }
    
    fun reset() {
        pulseHistory.clear()
        intensityHistory.clear()
    }
}
