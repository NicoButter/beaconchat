package com.nicobutter.beaconchat.transceiver

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class LightDetector(private val onLightStateChanged: (Boolean) -> Unit) : ImageAnalysis.Analyzer {

    private var lastState = false
    private var threshold = 150 // Initial threshold, can be dynamic
    private val history = ArrayDeque<Int>()
    private val historySize = 30

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = toByteArray(buffer)
        val pixels = data.map { it.toInt() and 0xFF }

        // Calculate average brightness of the center 10% of the image
        // This is a simplification. Ideally we crop the center.
        // Since we have the raw Y plane (luminance), we can just average the bytes.
        // To be efficient, let's just sample the center.

        val width = image.width
        val height = image.height
        val centerX = width / 2
        val centerY = height / 2
        val cropSize = minOf(width, height) / 10

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

        // If dynamic range is small, assume noise and keep previous state or default to OFF
        // If dynamic range is significant, set threshold in the middle
        if (max - min > 20) {
            threshold = (max + min) / 2
        }

        val newState = average > threshold

        if (newState != lastState) {
            lastState = newState
            onLightStateChanged(newState)
        }

        image.close()
    }

    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }
}
