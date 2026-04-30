package com.nicobutter.beaconchat.lightmap

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Detects light pulses from other BeaconChat devices with oscilloscope-style analysis.
 *
 * This analyzer processes camera frames to detect and analyze light signals from other
 * BeaconChat devices. It provides real-time signal analysis including:
 * - Real-time intensity curve generation (oscilloscope functionality)
 * - Precise heartbeat pattern detection
 * - Approximate flash position calculation (angle)
 * - Intensity/distance estimation
 *
 * The scanner uses CameraX ImageAnalysis to process frames at throttled rates for
 * optimal performance while maintaining detection accuracy.
 */
class LightScanner : ImageAnalysis.Analyzer {

    private val _detectedDevices = MutableStateFlow<List<DetectedDevice>>(emptyList())
    val detectedDevices: StateFlow<List<DetectedDevice>> = _detectedDevices.asStateFlow()

    // Intensity buffer for oscilloscope (last N frames)
    private val intensityBuffer = ArrayDeque<IntensityPoint>(OSCILLOSCOPE_BUFFER_SIZE)

    private val _signalData = MutableStateFlow<List<IntensityPoint>>(emptyList())
    val signalData: StateFlow<List<IntensityPoint>> = _signalData.asStateFlow()

    // Signal statistics
    private val _signalStats = MutableStateFlow(SignalStats())
    val signalStats: StateFlow<SignalStats> = _signalStats.asStateFlow()

    // Brightness history for detecting changes
    private val brightnessHistory = ArrayDeque<BrightnessFrame>(HISTORY_SIZE)

    // Temporary buffer for detected patterns
    private val patternBuffer = mutableListOf<FlashEvent>()

    // Last frame processing time (for FPS calculation)
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var lastFpsUpdate = 0L

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

    /**
     * Represents brightness data for a single camera frame.
     *
     * @property timestamp When this frame was captured
     * @property avgBrightness Average brightness across the entire frame
     * @property centerBrightness Brightness in the center region of the frame
     * @property flashPositionX Normalized horizontal position of brightest region (0.0-1.0)
     * @property flashPositionY Normalized vertical position of brightest region (0.0-1.0)
     */
    data class BrightnessFrame(
        val timestamp: Long,
        val avgBrightness: Int,
        val centerBrightness: Int,
        val flashPositionX: Float = 0.5f, // 0.0 to 1.0 (relative to width)
        val flashPositionY: Float = 0.5f  // 0.0 to 1.0 (relative to height)
    )

    /**
     * Represents a detected flash/light event.
     *
     * @property timestamp When the flash was detected
     * @property intensity Brightness level of the flash
     * @property positionX Normalized horizontal position (0.0-1.0)
     * @property positionY Normalized vertical position (0.0-1.0)
     * @property duration Duration of the flash event in milliseconds
     */
    data class FlashEvent(
        val timestamp: Long,
        val intensity: Int,
        val positionX: Float,
        val positionY: Float,
        val duration: Long = 0L
    )
    
    /**
     * Processes a camera frame to detect light signals and update analysis state.
     *
     * This method is called by CameraX for each frame. It performs the following operations:
     * 1. Throttles processing to ~15 FPS for optimal performance
     * 2. Extracts luminance data from the YUV image
     * 3. Updates intensity buffer for oscilloscope display
     * 4. Detects peaks and valleys in the signal
     * 5. Finds brightest regions for flash position detection
     * 6. Detects flash events and recognizes heartbeat patterns
     * 7. Updates signal statistics
     *
     * @param image The camera image proxy containing the frame data
     */
    override fun analyze(image: ImageProxy) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Skip frames if processing is too slow (throttle to max 15 FPS for better detection)
            if (currentTime - lastFrameTime < 66) { // ~15 FPS
                image.close()
                return
            }
            
            // Extract Y plane (luminance)
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)
            
            val width = image.width
            val height = image.height
            val rowStride = image.planes[0].rowStride
            
            // Calculate average brightness (with aggressive sampling for performance)
            val avgBrightness = calculateAverageBrightness(data)
            
            // Add to oscilloscope buffer
            val intensityPoint = IntensityPoint(
                timestamp = currentTime,
                intensity = avgBrightness
            )
            intensityBuffer.addLast(intensityPoint)
            if (intensityBuffer.size > OSCILLOSCOPE_BUFFER_SIZE) {
                intensityBuffer.removeFirst()
            }
            
            // Detect peaks and valleys in the oscilloscope buffer
            if (intensityBuffer.size >= 5) {
                markPeaksAndValleys()
            }
            
            // Update StateFlow for the mini-oscilloscope
            _signalData.value = intensityBuffer.toList()
            
            // Calculate center brightness (for reference)
            val centerBrightness = calculateCenterBrightness(data, width, height, rowStride)
            
            // Detect position of brightest point (possible flash)
            val flashPosition = findBrightestRegion(data, width, height, rowStride)
            
            // Add frame to history
            val frame = BrightnessFrame(
                timestamp = currentTime,
                avgBrightness = avgBrightness,
                centerBrightness = centerBrightness,
                flashPositionX = flashPosition.first,
                flashPositionY = flashPosition.second
            )
            
            brightnessHistory.addLast(frame)
            if (brightnessHistory.size > HISTORY_SIZE) {
                brightnessHistory.removeFirst()
            }
            
            // Detect sudden brightness changes (flashes)
            if (brightnessHistory.size >= 3) {
                detectFlashes()
            }
            
            // Try to recognize patterns (heartbeats) - only every 5 frames
            if (brightnessHistory.size % 5 == 0) {
                recognizePatterns()
            }
            
            // Update signal statistics
            updateSignalStats(currentTime)
            
            lastFrameTime = currentTime
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame", e)
        } finally {
            image.close()
        }
    }
    
    /**
     * Calculates the average brightness across the entire image frame.
     *
     * Uses aggressive sampling (every 50th pixel) for performance optimization
     * while maintaining reasonable accuracy for signal detection.
     *
     * @param data Raw image data from the Y plane
     * @return Average brightness value (0-255)
     */
    private fun calculateAverageBrightness(data: ByteArray): Int {
        var sum = 0L
        // Much more aggressive sampling: every 50 pixels (was 10 before)
        for (i in data.indices step 50) {
            sum += (data[i].toInt() and 0xFF)
        }
        return (sum / (data.size / 50)).toInt()
    }
    
    private fun calculateCenterBrightness(
        data: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int
    ): Int {
        val centerX = width / 2
        val centerY = height / 2
        val regionSize = minOf(width, height) / 10
        
        var sum = 0L
        var count = 0
        
        for (y in (centerY - regionSize)..(centerY + regionSize)) {
            for (x in (centerX - regionSize)..(centerX + regionSize)) {
                if (y >= 0 && y < height && x >= 0 && x < width) {
                    val index = y * rowStride + x
                    if (index < data.size) {
                        sum += (data[index].toInt() and 0xFF)
                        count++
                    }
                }
            }
        }
        
        return if (count > 0) (sum / count).toInt() else 0
    }
    
    private fun findBrightestRegion(
        data: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int
    ): Pair<Float, Float> {
        // Reducir grid de 5x5 a 3x3 para mejor performance
        val gridSize = 3
        val cellWidth = width / gridSize
        val cellHeight = height / gridSize
        
        var maxBrightness = 0
        var maxX = 0.5f
        var maxY = 0.5f
        
        for (gy in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                var sum = 0L
                var count = 0
                
                val startX = gx * cellWidth
                val startY = gy * cellHeight
                val endX = minOf(startX + cellWidth, width)
                val endY = minOf(startY + cellHeight, height)
                
                // Muestreo más agresivo: cada 10 píxeles (antes era 5)
                for (y in startY until endY step 10) {
                    for (x in startX until endX step 10) {
                        val index = y * rowStride + x
                        if (index < data.size) {
                            sum += (data[index].toInt() and 0xFF)
                            count++
                        }
                    }
                }
                
                val avgBrightness = if (count > 0) (sum / count).toInt() else 0
                
                if (avgBrightness > maxBrightness) {
                    maxBrightness = avgBrightness
                    maxX = (gx + 0.5f) / gridSize
                    maxY = (gy + 0.5f) / gridSize
                }
            }
        }
        
        return Pair(maxX, maxY)
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
    
    private fun updateSignalStats(currentTime: Long) {
        if (intensityBuffer.isEmpty()) return
        
        // Calcular FPS cada segundo
        val fps = if (currentTime - lastFpsUpdate >= 1000) {
            val calculatedFps = frameCount
            frameCount++
            lastFpsUpdate = currentTime
            calculatedFps
        } else {
            frameCount++
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
    
    /**
     * Detects sudden brightness increases that indicate flash events.
     *
     * Compares the current frame brightness against the average of recent frames
     * to identify significant increases above the FLASH_THRESHOLD. Detected flashes
     * are added to the pattern buffer for heartbeat recognition.
     */
    private fun detectFlashes() {
        if (brightnessHistory.size < 5) return
        
        val latest = brightnessHistory.last()
        
        // Calculate average of last 4 frames (excluding current)
        val recentFrames = brightnessHistory.takeLast(5).dropLast(1)
        val avgRecent = recentFrames.map { it.avgBrightness }.average().toInt()
        
        // Detect sudden increase compared to recent average (reduces noise)
        val increase = latest.avgBrightness - avgRecent
        
        if (increase > FLASH_THRESHOLD) {
            val flashEvent = FlashEvent(
                timestamp = latest.timestamp,
                intensity = latest.avgBrightness,
                positionX = latest.flashPositionX,
                positionY = latest.flashPositionY
            )
            
            patternBuffer.add(flashEvent)
            
            Log.d(TAG, "Flash detected! Intensity: ${latest.avgBrightness} (avg recent: $avgRecent), Position: (${latest.flashPositionX}, ${latest.flashPositionY})")
            
            // Clean old buffer (keep only last 5 seconds)
            val cutoff = System.currentTimeMillis() - 5000
            patternBuffer.removeAll { it.timestamp < cutoff }
        }
    }
    
    /**
     * Attempts to recognize heartbeat patterns from detected flash events.
     *
     * Looks for patterns of 3+ flashes with relatively uniform intervals (100-500ms)
     * that match the BeaconChat heartbeat pattern. When a pattern is recognized,
     * it calculates the device position and creates/updates a DetectedDevice entry.
     */
    private fun recognizePatterns() {
        if (patternBuffer.size < 3) return
        
        // Look for heartbeat pattern: 3 short pulses (simplified SOS or ID)
        // This is a basic example - can be improved to recognize more complex patterns
        
        val recentFlashes = patternBuffer.takeLast(5)
        if (recentFlashes.size >= 3) {
            val timeDiffs = mutableListOf<Long>()
            for (i in 1 until recentFlashes.size) {
                timeDiffs.add(recentFlashes[i].timestamp - recentFlashes[i - 1].timestamp)
            }
            
            // If intervals are relatively uniform (~100-300ms), could be a heartbeat
            val avgInterval = timeDiffs.average()
            if (avgInterval in 100.0..500.0) {
                val lastFlash = recentFlashes.last()
                
                // Calculate approximate angle based on position in frame
                val angle = calculateAngle(lastFlash.positionX, lastFlash.positionY)
                
                // Estimate distance based on intensity (very approximate)
                val estimatedDistance = estimateDistance(lastFlash.intensity)
                
                // Create/update detected device
                updateDetectedDevice(
                    deviceId = "DEV_${lastFlash.positionX}_${lastFlash.positionY}".hashCode().toString(),
                    timestamp = lastFlash.timestamp,
                    intensity = lastFlash.intensity,
                    angle = angle,
                    distance = estimatedDistance,
                    positionX = lastFlash.positionX,
                    positionY = lastFlash.positionY
                )
                
                Log.d(TAG, "Heartbeat pattern recognized! Angle: $angle°, Distance: ~${estimatedDistance}m")
            }
        }
    }
    
    private fun calculateAngle(posX: Float, posY: Float): Float {
        // Convertir posición en frame (0-1) a ángulo en grados
        // En coordenadas de cámara/display:
        // posX = 0 es IZQUIERDA, posX = 1 es DERECHA
        // posY = 0 es ARRIBA, posY = 1 es ABAJO
        
        // El radar usa coordenadas polares estándar en Canvas:
        // 0 grados es DERECHA (coincide con posX = 1)
        // 90 grados es ABAJO (coincide con posY = 1)
        // 180 grados es IZQUIERDA (coincide con posX = 0)
        // 270/-90 grados es ARRIBA (coincide con posY = 0)
        
        val dx = posX - 0.5f  // -0.5 a 0.5
        val dy = posY - 0.5f  // -0.5 a 0.5
        
        // atan2(y, x) devuelve el ángulo desde el eje X positivo (derecha)
        val angleRad = atan2(dy.toDouble(), dx.toDouble())
        var angleDeg = Math.toDegrees(angleRad).toFloat()
        
        // Asegurar rango 0-360 para consistencia con getDirection()
        if (angleDeg < 0) angleDeg += 360f
        
        return angleDeg
    }
    
    private fun estimateDistance(intensity: Int): Float {
        // Estimación muy aproximada basada en intensidad
        // Esto asume que la intensidad decae con el cuadrado de la distancia
        // En la práctica necesitaría calibración
        
        return when {
            intensity > 200 -> 1.0f // Muy cerca
            intensity > 150 -> 2.0f
            intensity > 100 -> 3.5f
            intensity > 70 -> 5.0f
            else -> 10.0f // Lejos o débil
        }
    }
    
    private fun updateDetectedDevice(
        deviceId: String,
        timestamp: Long,
        intensity: Int,
        angle: Float,
        distance: Float,
        positionX: Float,
        positionY: Float
    ) {
        val currentDevices = _detectedDevices.value.toMutableList()
        
        val existingIndex = currentDevices.indexOfFirst { it.id == deviceId }
        
        if (existingIndex >= 0) {
            // Actualizar dispositivo existente
            val existing = currentDevices[existingIndex]
            currentDevices[existingIndex] = existing.copy(
                lastSeen = timestamp,
                intensity = intensity,
                angle = angle,
                estimatedDistance = distance,
                detectionCount = existing.detectionCount + 1
            )
        } else {
            // Nuevo dispositivo
            currentDevices.add(
                DetectedDevice(
                    id = deviceId,
                    firstSeen = timestamp,
                    lastSeen = timestamp,
                    intensity = intensity,
                    angle = angle,
                    estimatedDistance = distance,
                    positionX = positionX,
                    positionY = positionY,
                    detectionCount = 1
                )
            )
        }
        
        // Limpiar dispositivos viejos (no vistos en más de 30 segundos)
        val cutoff = System.currentTimeMillis() - 30000
        currentDevices.removeAll { it.lastSeen < cutoff }
        
        _detectedDevices.value = currentDevices
    }
    
    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
    
    /**
     * Resets all analysis state and clears detected devices.
     *
     * Clears all buffers, histories, and detected devices. Useful when
     * restarting analysis or switching between different scanning modes.
     */
    fun reset() {
        brightnessHistory.clear()
        patternBuffer.clear()
        intensityBuffer.clear()
        _detectedDevices.value = emptyList()
        _signalData.value = emptyList()
        _signalStats.value = SignalStats()
    }
    
    companion object {
        private const val TAG = "LightScanner"
        private const val HISTORY_SIZE = 10
        private const val FLASH_THRESHOLD = 20 // Lower threshold to detect weaker flashes
        private const val OSCILLOSCOPE_BUFFER_SIZE = 150 // ~5 seconds at 30 FPS for mini-oscilloscope
    }
}
