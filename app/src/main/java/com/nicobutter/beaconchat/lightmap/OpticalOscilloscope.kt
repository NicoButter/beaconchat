package com.nicobutter.beaconchat.lightmap

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

/**
 * Converts the camera into an optical oscilloscope for real-time signal analysis.
 *
 * This analyzer reads light intensity in real-time and generates a signal curve
 * that can decode Morse code, binary patterns, and other signals with surgical precision.
 * It focuses on the center region of the camera frame for optimal signal detection.
 */
class OpticalOscilloscope : ImageAnalysis.Analyzer {
    
    // Circular buffer of intensities (last N frames)
    private val intensityBuffer = ArrayDeque<IntensityPoint>(BUFFER_SIZE)
    
    // StateFlow for the UI
    private val _signalData = MutableStateFlow<List<IntensityPoint>>(emptyList())
    val signalData: StateFlow<List<IntensityPoint>> = _signalData.asStateFlow()
    
    // Detection of decoded messages
    private val _decodedMessage = MutableStateFlow<String>("")
    val decodedMessage: StateFlow<String> = _decodedMessage.asStateFlow()
    
    // Signal statistics
    private val _signalStats = MutableStateFlow(SignalStats())
    val signalStats: StateFlow<SignalStats> = _signalStats.asStateFlow()
    
    // Pulse detector (for Morse code)
    private val pulseDetector = PulseDetector()
    
    /**
     * Represents a single intensity measurement point for oscilloscope display.
     *
     * @property timestamp When this measurement was taken
     * @property intensity Light intensity value (0-255)
     * @property isPeak Whether this point represents a local maximum
     * @property isValley Whether this point represents a local minimum
     */
    data class IntensityPoint(
        val timestamp: Long,
        val intensity: Int, // 0-255
        val isPeak: Boolean = false,
        val isValley: Boolean = false
    )
    
    /**
     * Real-time signal statistics for monitoring and debugging.
     *
     * @property fps Current frames per second processing rate
     * @property avgIntensity Average intensity across recent frames
     * @property minIntensity Minimum intensity in recent frames
     * @property maxIntensity Maximum intensity in recent frames
     * @property noiseLevel Signal noise level (standard deviation)
     */
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
    
    /**
     * Processes a camera frame to analyze light intensity and decode signals.
     *
     * Extracts intensity from the center region of the frame, updates the signal buffer,
     * detects peaks/valleys for visualization, attempts Morse code decoding, and
     * calculates real-time statistics.
     *
     * @param image The camera image proxy containing the frame data
     */
    override fun analyze(image: ImageProxy) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Extract average intensity from frame (centered ROI like LightDetector)
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)
            val intensity = calculateCenterIntensity(image, data)
            
            // Create intensity point
            val point = IntensityPoint(
                timestamp = currentTime,
                intensity = intensity
            )
            
            // Add to circular buffer
            intensityBuffer.addLast(point)
            if (intensityBuffer.size > BUFFER_SIZE) {
                intensityBuffer.removeFirst()
            }
            
            // Detect peaks and valleys (for visualization and analysis)
            if (intensityBuffer.size >= 5) {
                markPeaksAndValleys()
            }
            
            // Update StateFlow for the UI
            _signalData.value = intensityBuffer.toList()
            
            // Morse pulse detector
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
            
            // Calculate statistics
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
    
    /**
     * Resets all analysis state and clears buffers.
     *
     * Clears the intensity buffer, decoded messages, statistics, and resets
     * the pulse detector. Useful when restarting analysis or switching modes.
     */
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
        private const val BUFFER_SIZE = 300 // ~10 seconds at 30 FPS
    }
}

/**
 * Detects and decodes Morse code pulses from light intensity signals.
 *
 * This class analyzes intensity curves to identify dots, dashes, and gaps
 * that form Morse code patterns. It uses dynamic thresholding based on
 * recent signal history for robust detection in varying lighting conditions.
 */
class PulseDetector {
    private val pulseHistory = mutableListOf<Pulse>()
    private val intensityHistory = ArrayDeque<Int>()
    private val historySize = 30 // Last 30 frames for calculating threshold
    
    /**
     * Represents a detected pulse in the signal.
     *
     * @property startTime When the pulse began
     * @property endTime When the pulse ended
     * @property duration Total duration of the pulse in milliseconds
     * @property type The type of pulse (DOT, DASH, or GAP)
     */
    data class Pulse(
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val type: PulseType
    )
    
    /**
     * Enumeration of different pulse types in Morse code.
     */
    enum class PulseType {
        DOT,    // Short pulse
        DASH,   // Long pulse
        GAP     // Silence between pulses
    }
    
    /**
     * Analyzes a signal curve to detect Morse code pulses.
     *
     * Uses dynamic thresholding based on recent intensity history to identify
     * transitions between light and dark states. Filters out noise by ignoring
     * very short pulses.
     *
     * @param signal List of intensity points representing the signal curve
     * @return List of detected pulses with timing and type information
     */
    fun detectPulses(signal: List<OpticalOscilloscope.IntensityPoint>): List<Pulse> {
        val pulses = mutableListOf<Pulse>()
        
        // Update intensity history for dynamic threshold
        signal.forEach { point ->
            intensityHistory.addLast(point.intensity)
            if (intensityHistory.size > historySize) {
                intensityHistory.removeFirst()
            }
        }
        
        // Calculate dynamic threshold based on min/max like LightDetector
        val min = intensityHistory.minOrNull() ?: 0
        val max = intensityHistory.maxOrNull() ?: 255
        
        // If there's enough dynamic range, use threshold in the middle
        // If not, use a threshold based on average
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
                // Start of pulse (ON)
                inPulse = true
                pulseStart = point.timestamp
            } else if (inPulse && point.intensity < threshold) {
                // End of pulse (OFF)
                inPulse = false
                val duration = point.timestamp - pulseStart
                
                if (duration > 30) { // Ignore very short pulses (noise)
                    val type = if (duration < 150) PulseType.DOT else PulseType.DASH
                    pulses.add(Pulse(pulseStart, point.timestamp, duration, type))
                }
            }
        }
        
        return pulses
    }
    
    /**
     * Decodes a list of pulses into readable text using Morse code.
     *
     * Converts the pulse sequence into Morse code symbols and then
     * translates them into letters and words.
     *
     * @param pulses List of detected pulses to decode
     * @return Decoded text message
     */
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
    
    /**
     * Resets the pulse detector state.
     *
     * Clears all pulse history and intensity history buffers.
     */
    fun reset() {
        pulseHistory.clear()
        intensityHistory.clear()
    }
}
