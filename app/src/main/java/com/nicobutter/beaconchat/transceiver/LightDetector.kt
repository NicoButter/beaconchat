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
    
    // HYSTERESIS: State confirmation buffer
    // Require 2 consecutive frames with same state to confirm change
    // Prevents flickering from single-frame noise
    private var stateConfirmationBuffer = ArrayDeque<Boolean>()
    private val confirmationFrames = 2
    
    // EDGE DETECTION: Track rapid brightness changes
    // Fast transitions indicate real LED state change, not gradual ambient changes
    private var lastBrightness = 0
    
    // AMPLITUDE FILTERING: Reject weak pulses from fading light
    // Camera sensor persistence creates ghost signals - filter by amplitude
    private val minimumPeakAmplitude = 60 // Minimum brightness difference to accept as valid pulse
    
    // FADING DETECTION: Track gradual brightness decay
    // LED turns off instantly, but camera sees fading - detect and force OFF
    private var brightnessDecayRate = 0

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

        // AMPLITUDE FILTERING: Calculate signal amplitude
        val amplitude = max - min
        val hasValidAmplitude = amplitude >= minimumPeakAmplitude

        // AGGRESSIVE THRESHOLD: Use 60% of range instead of 50% to reject weak signals
        // If dynamic range is small, assume noise. If significant, set threshold higher.
        if (amplitude > 15) {
            threshold = min + (amplitude * 0.6).toInt()  // 60% threshold instead of 50%
        }
        
        // EDGE DETECTION: Calculate brightness change rate
        val brightnessDelta = average.toInt() - lastBrightness
        
        // FADING DETECTION: Track gradual decay
        brightnessDecayRate = if (brightnessDelta < 0) brightnessDelta else 0
        val isFading = brightnessDecayRate < -5 && average < threshold
        
        lastBrightness = average.toInt()
        
        // Determine potential new state with edge detection optimization
        val potentialNewState = when {
            // FADING LIGHT: Force OFF if gradual decay detected
            isFading -> false
            
            // AMPLITUDE FILTER: Reject if signal too weak (likely ghost from camera lag)
            !hasValidAmplitude && amplitude > 0 -> lastState // Keep current state if amplitude too weak
            
            // EDGE DETECTION: Fast confirm ON if sharp brightness increase
            brightnessDelta > 40 -> true
            
            // EDGE DETECTION: Fast confirm OFF if sharp brightness decrease  
            brightnessDelta < -20 -> false
            
            // NORMAL: Standard threshold comparison
            else -> average > threshold
        }

        // HYSTERESIS: Confirm state change with buffer
        stateConfirmationBuffer.addLast(potentialNewState)
        if (stateConfirmationBuffer.size > confirmationFrames) {
            stateConfirmationBuffer.removeFirst()
        }
        
        // Only change state if all recent frames agree (hysteresis)
        // OR if edge detection detected sharp transition
        val shouldChangeState = when {
            // Fast path: Sharp edge detected
            brightnessDelta > 40 || brightnessDelta < -20 -> potentialNewState != lastState
            
            // Normal path: Require confirmation frames
            stateConfirmationBuffer.size == confirmationFrames -> {
                val allAgree = stateConfirmationBuffer.all { it == potentialNewState }
                allAgree && potentialNewState != lastState
            }
            
            else -> false
        }

        if (shouldChangeState) {
            lastState = potentialNewState
            onLightStateChanged(potentialNewState)
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
