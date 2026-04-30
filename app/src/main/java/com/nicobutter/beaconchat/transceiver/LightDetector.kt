package com.nicobutter.beaconchat.transceiver

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Detects light ON/OFF transitions from camera frames.
 * Measures wall-clock duration of each state change.
 *
 * Measures wall-clock time (milliseconds) for each state duration.
 * Simple threshold-based ON/OFF detection.
 */
class LightDetector(
    private val onLightStateChanged: (Boolean, Long) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastState = false
    private var stateChangeTime = 0L
    private val history = ArrayDeque<Int>()
    private val historySize = 20

    private var threshold = 128
    private var frameCount = 0
    private var debugLogInterval = 10 // Log every N frames

    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)
            val width = image.width
            val height = image.height
            val rowStride = image.planes[0].rowStride
            
            // Get center ROI (30% of image)
            val centerX = width / 2
            val centerY = height / 2
            val roiSize = minOf(width, height) / 3
            val roiX = maxOf(0, centerX - roiSize / 2)
            val roiY = maxOf(0, centerY - roiSize / 2)
            val roiWidth = minOf(roiSize, width - roiX)
            val roiHeight = minOf(roiSize, height - roiY)
            
            // Calculate average brightness in ROI
            var sumBrightness = 0
            var count = 0
            for (y in roiY until (roiY + roiHeight)) {
                for (x in roiX until (roiX + roiWidth)) {
                    val index = y * rowStride + x
                    if (index < data.size) {
                        sumBrightness += data[index].toInt() and 0xFF
                        count++
                    }
                }
            }
            
            val brightness = if (count > 0) sumBrightness / count else 0
            
            // Update history
            history.addLast(brightness)
            if (history.size > historySize) {
                history.removeFirst()
            }
            
            // Adaptive threshold (only update with sufficient history)
            if (history.size >= historySize) {
                val minBright = history.minOrNull() ?: 0
                val maxBright = history.maxOrNull() ?: 255
                threshold = (minBright + maxBright) / 2
            }
            
            // Simple ON/OFF detection
            val isLightOn = brightness > threshold
            
            // Debug logging
            frameCount++
            if (frameCount % debugLogInterval == 0) {
                val minH = history.minOrNull() ?: 0
                val maxH = history.maxOrNull() ?: 255
                Log.d("LightDetector", "Frame $frameCount: brightness=$brightness, threshold=$threshold, min=$minH, max=$maxH, state=$isLightOn")
            }
            
            // Detect state change
            if (isLightOn != lastState) {
                val now = System.currentTimeMillis()
                val duration = if (stateChangeTime == 0L) 0L else now - stateChangeTime
                Log.w("LightDetector", "STATE CHANGE: $lastState → $isLightOn, duration: ${duration}ms, brightness: $brightness, threshold: $threshold")
                onLightStateChanged(isLightOn, duration)
                stateChangeTime = now
                lastState = isLightOn
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LightDetector", "Processing error: ${e.message}")
        } finally {
            image.close()
        }
    }
    

    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
}
