package com.nicobutter.beaconchat.lightmap

import kotlin.math.roundToInt

/**
 * Representa un dispositivo BeaconChat detectado mediante señales luminosas
 */
data class DetectedDevice(
    val id: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val intensity: Int,
    val angle: Float, // Ángulo en grados (-180 a 180)
    val estimatedDistance: Float, // Distancia estimada en metros
    val positionX: Float, // Posición normalizada en el frame (0-1)
    val positionY: Float, // Posición normalizada en el frame (0-1)
    val detectionCount: Int = 1
) {
    /**
     * Tiempo desde última detección en segundos
     */
    fun timeSinceLastSeen(): String {
        val seconds = ((System.currentTimeMillis() - lastSeen) / 1000).toInt()
        return when {
            seconds < 5 -> "ahora"
            seconds < 60 -> "${seconds}s"
            else -> "${seconds / 60}m"
        }
    }
    
    /**
     * Calidad de la señal basada en intensidad
     */
    fun getSignalQuality(): String {
        return when {
            intensity > 200 -> "Excelente"
            intensity > 150 -> "Buena"
            intensity > 100 -> "Regular"
            else -> "Débil"
        }
    }
    
    /**
     * Dirección cardinal aproximada
     */
    fun getDirection(): String {
        val normalizedAngle = ((angle + 360) % 360).toInt()
        return when (normalizedAngle) {
            in 0..22, in 338..360 -> "E"
            in 23..67 -> "NE"
            in 68..112 -> "N"
            in 113..157 -> "NW"
            in 158..202 -> "W"
            in 203..247 -> "SW"
            in 248..292 -> "S"
            in 293..337 -> "SE"
            else -> "?"
        }
    }
    
    /**
     * Distancia formateada para display
     */
    fun getDistanceString(): String {
        return when {
            estimatedDistance < 1 -> "${(estimatedDistance * 100).roundToInt()}cm"
            estimatedDistance < 10 -> "${"%.1f".format(estimatedDistance)}m"
            else -> "${estimatedDistance.roundToInt()}m"
        }
    }
    
    /**
     * Descripción completa del dispositivo
     */
    fun getDescription(): String {
        return "Dir: ${getDirection()} • Dist: ${getDistanceString()} • Señal: ${getSignalQuality()}"
    }
}
