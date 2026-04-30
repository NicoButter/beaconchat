package com.nicobutter.beaconchat.lightmap

import kotlin.math.roundToInt

/**
 * Represents a BeaconChat device detected through light signals.
 *
 * This data class encapsulates all the information about a device that has been detected
 * by analyzing light patterns from the camera feed. It includes positional data, signal
 * quality metrics, and timing information to help determine device proximity and status.
 *
 * @property id Unique identifier for the detected device
 * @property firstSeen Timestamp when the device was first detected (milliseconds since epoch)
 * @property lastSeen Timestamp when the device was last detected (milliseconds since epoch)
 * @property intensity Average light intensity of the detected signal (0-255)
 * @property angle Angular position relative to the camera in degrees (-180 to 180)
 * @property estimatedDistance Estimated distance to the device in meters
 * @property positionX Normalized horizontal position in the camera frame (0.0 to 1.0)
 * @property positionY Normalized vertical position in the camera frame (0.0 to 1.0)
 * @property detectionCount Number of times this device has been detected
 */
data class DetectedDevice(
    val id: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val intensity: Int,
    val angle: Float, // Angle in degrees (-180 to 180)
    val estimatedDistance: Float, // Estimated distance in meters
    val positionX: Float, // Normalized position in frame (0-1)
    val positionY: Float, // Normalized position in frame (0-1)
    val detectionCount: Int = 1
) {
    /**
     * Returns a human-readable string representing time since last detection.
     *
     * @return Time string in format like "ahora" (now), "5s", or "2m"
     */
    fun timeSinceLastSeen(): String {
        val seconds = ((System.currentTimeMillis() - lastSeen) / 1000).toInt()
        return when {
            seconds < 5 -> "now"
            seconds < 60 -> "${seconds}s"
            else -> "${seconds / 60}m"
        }
    }

    /**
     * Returns signal quality based on light intensity.
     *
     * @return Quality description: "Excellent", "Good", "Fair", or "Weak"
     */
    fun getSignalQuality(): String {
        return when {
            intensity > 200 -> "Excellent"
            intensity > 150 -> "Good"
            intensity > 100 -> "Fair"
            else -> "Weak"
        }
    }

    /**
     * Returns approximate cardinal direction based on angle.
     * El radar usa: 0=Derecha(E), 90=Abajo(S), 180=Izquierda(W), 270=Arriba(N)
     *
     * @return Cardinal direction abbreviation (N, NE, E, SE, S, SW, W, NW)
     */
    fun getDirection(): String {
        val normalizedAngle = ((angle + 360) % 360).toInt()
        return when (normalizedAngle) {
            in 0..22, in 338..360 -> "E"   // Derecha
            in 23..67 -> "SE"              // Derecha-Abajo
            in 68..112 -> "S"              // Abajo
            in 113..157 -> "SW"            // Izquierda-Abajo
            in 158..202 -> "W"             // Izquierda
            in 203..247 -> "NW"            // Izquierda-Arriba
            in 248..292 -> "N"              // Arriba
            in 293..337 -> "NE"            // Derecha-Arriba
            else -> "?"
        }
    }

    /**
     * Returns formatted distance string for display.
     *
     * @return Distance string in format like "50cm", "2.5m", or "15m"
     */
    fun getDistanceString(): String {
        return when {
            estimatedDistance < 1 -> "${(estimatedDistance * 100).roundToInt()}cm"
            estimatedDistance < 10 -> "${"%.1f".format(estimatedDistance)}m"
            else -> "${estimatedDistance.roundToInt()}m"
        }
    }

    /**
     * Returns complete device description combining direction, distance, and signal quality.
     *
     * @return Formatted description string like "Dir: N • Dist: 5.2m • Signal: Good"
     */
    fun getDescription(): String {
        return "Dir: ${getDirection()} • Dist: ${getDistanceString()} • Signal: ${getSignalQuality()}"
    }
}
