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
 * LightScanner - Detecta pulsos luminosos de otros dispositivos BeaconChat
 * 
 * Analiza frames de cámara para:
 * - Detectar cambios súbitos de luminosidad (flashes)
 * - Calcular posición aproximada del flash en el frame (ángulo)
 * - Estimar intensidad/distancia
 * - Reconocer patrones de heartbeat
 */
class LightScanner : ImageAnalysis.Analyzer {
    
    private val _detectedDevices = MutableStateFlow<List<DetectedDevice>>(emptyList())
    val detectedDevices: StateFlow<List<DetectedDevice>> = _detectedDevices.asStateFlow()
    
    // Historial de brillos para detectar cambios
    private val brightnessHistory = ArrayDeque<BrightnessFrame>(HISTORY_SIZE)
    
    // Buffer temporal para patrones detectados
    private val patternBuffer = mutableListOf<FlashEvent>()
    
    // Última vez que se procesó un frame (para calcular FPS)
    private var lastFrameTime = 0L
    
    data class BrightnessFrame(
        val timestamp: Long,
        val avgBrightness: Int,
        val centerBrightness: Int,
        val flashPositionX: Float = 0.5f, // 0.0 a 1.0 (relativo al ancho)
        val flashPositionY: Float = 0.5f  // 0.0 a 1.0 (relativo al alto)
    )
    
    data class FlashEvent(
        val timestamp: Long,
        val intensity: Int,
        val positionX: Float,
        val positionY: Float,
        val duration: Long = 0L
    )
    
    override fun analyze(image: ImageProxy) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Skip frames if processing is too slow (throttle to max 10 FPS)
            if (currentTime - lastFrameTime < 100) {
                image.close()
                return
            }
            
            // Extraer plano Y (luminancia)
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)
            
            val width = image.width
            val height = image.height
            val rowStride = image.planes[0].rowStride
            
            // Calcular brillo promedio general (con muestreo agresivo para performance)
            val avgBrightness = calculateAverageBrightness(data)
            
            // Calcular brillo en centro (para referencia)
            val centerBrightness = calculateCenterBrightness(data, width, height, rowStride)
            
            // Detectar posición del punto más brillante (posible flash)
            val flashPosition = findBrightestRegion(data, width, height, rowStride)
            
            // Agregar frame al historial
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
            
            // Detectar cambios bruscos de luminosidad (flashes)
            if (brightnessHistory.size >= 3) {
                detectFlashes()
            }
            
            // Intentar reconocer patrones (heartbeats) - solo cada 5 frames
            if (brightnessHistory.size % 5 == 0) {
                recognizePatterns()
            }
            
            lastFrameTime = currentTime
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame", e)
        } finally {
            image.close()
        }
    }
    
    private fun calculateAverageBrightness(data: ByteArray): Int {
        var sum = 0L
        // Muestreo mucho más agresivo: cada 50 píxeles (antes era 10)
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
    
    private fun detectFlashes() {
        if (brightnessHistory.size < 3) return
        
        val latest = brightnessHistory.last()
        val previous = brightnessHistory[brightnessHistory.size - 2]
        
        // Detectar subida brusca (inicio de flash)
        val increase = latest.avgBrightness - previous.avgBrightness
        
        if (increase > FLASH_THRESHOLD) {
            val flashEvent = FlashEvent(
                timestamp = latest.timestamp,
                intensity = latest.avgBrightness,
                positionX = latest.flashPositionX,
                positionY = latest.flashPositionY
            )
            
            patternBuffer.add(flashEvent)
            
            Log.d(TAG, "Flash detected! Intensity: ${latest.avgBrightness}, Position: (${latest.flashPositionX}, ${latest.flashPositionY})")
            
            // Limpiar buffer viejo (mantener solo últimos 5 segundos)
            val cutoff = System.currentTimeMillis() - 5000
            patternBuffer.removeAll { it.timestamp < cutoff }
        }
    }
    
    private fun recognizePatterns() {
        if (patternBuffer.size < 3) return
        
        // Buscar patrón de heartbeat: 3 pulsos cortos (SOS simplificado o ID)
        // Esto es un ejemplo básico - se puede mejorar para reconocer patrones más complejos
        
        val recentFlashes = patternBuffer.takeLast(5)
        if (recentFlashes.size >= 3) {
            val timeDiffs = mutableListOf<Long>()
            for (i in 1 until recentFlashes.size) {
                timeDiffs.add(recentFlashes[i].timestamp - recentFlashes[i - 1].timestamp)
            }
            
            // Si los intervalos son relativamente uniformes (~100-300ms), podría ser un heartbeat
            val avgInterval = timeDiffs.average()
            if (avgInterval in 100.0..500.0) {
                val lastFlash = recentFlashes.last()
                
                // Calcular ángulo aproximado basado en posición en el frame
                val angle = calculateAngle(lastFlash.positionX, lastFlash.positionY)
                
                // Estimar distancia basada en intensidad (muy aproximado)
                val estimatedDistance = estimateDistance(lastFlash.intensity)
                
                // Crear/actualizar dispositivo detectado
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
        // Centro del frame = 0°, derecha = 90°, izquierda = -90°
        val centerX = 0.5f
        val centerY = 0.5f
        
        val dx = posX - centerX
        val dy = posY - centerY
        
        // atan2 devuelve radianes, convertir a grados
        val angleRad = atan2(dy.toDouble(), dx.toDouble())
        val angleDeg = Math.toDegrees(angleRad).toFloat()
        
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
    
    fun reset() {
        brightnessHistory.clear()
        patternBuffer.clear()
        _detectedDevices.value = emptyList()
    }
    
    companion object {
        private const val TAG = "LightScanner"
        private const val HISTORY_SIZE = 10
        private const val FLASH_THRESHOLD = 40 // Umbral de cambio de brillo para detectar flash
    }
}
