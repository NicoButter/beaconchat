package com.nicobutter.beaconchat.transceiver

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Detects light state changes from camera frames for optical signaling.
 *
 * This analyzer processes camera image frames to detect changes in brightness
 * levels, particularly useful for detecting flashlight signals. It uses dynamic
 * thresholding based on recent brightness history to adapt to changing lighting
 * conditions and samples the center region of the image for reliable detection.
 */
class LightDetector(private val onLightStateChanged: (Boolean) -> Unit) : ImageAnalysis.Analyzer {

    private var lastState = false
    private var threshold = 150 // Initial threshold, can be dynamic
    private val history = ArrayDeque<Int>()
    // Reduce history size so threshold adapts faster to changing light conditions
    private val historySize = 8

    /**
     * Analyzes a camera image frame for brightness changes.
     *
     * Samples the center region of the YUV luminance plane to calculate average
     * brightness. Uses dynamic thresholding based on recent brightness history
     * to detect significant light state changes while filtering out noise.
     *
     * @param image The camera image frame to analyze
     */
    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
    val data = toByteArray(buffer)

    // Calculate average brightness of the center region of the image.
    // Widen the sampled ROI from 10% to ~16% to increase chance of including the flashlight
    // even if the user isn't perfectly centered.
        // Since we have the raw Y plane (luminance), we can just average the bytes.
        // To be efficient, let's just sample the center.

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

        // Stride is important for correct pixel access
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

        val average = if (count > 0) sum / count else 0

        // Dynamic thresholding
        history.addLast(average.toInt())
        if (history.size > historySize) {
            history.removeFirst()
        }

        val min = history.minOrNull() ?: 0
        val max = history.maxOrNull() ?: 255

        // If dynamic range is small, assume noise. If significant, set threshold in the middle.
        // With smaller history, accept a slightly smaller required range.
        if (max - min > 15) {
            threshold = (max + min) / 2
        }

        val newState = average > threshold

        if (newState != lastState) {
            lastState = newState
            onLightStateChanged(newState)
        }

        image.close()
    }

    /**
     * Converts a ByteBuffer to a byte array for image processing.
     *
     * Extracts all remaining bytes from the buffer into a new array,
     * rewinding the buffer position to the beginning first.
     *
     * @param buffer The ByteBuffer to convert
     * @return Byte array containing the buffer's data
     */
    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
}
