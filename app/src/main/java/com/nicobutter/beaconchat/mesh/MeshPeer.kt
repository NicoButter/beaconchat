package com.nicobutter.beaconchat.mesh

data class MeshPeer(
        val id: String, // Bluetooth address or device ID
        val name: String, // Device name
        val callsign: String, // User callsign
        val rssi: Int, // Signal strength (dBm)
        val lastSeen: Long // Timestamp in milliseconds
) {
    fun getSignalQuality(): String {
        return when {
            rssi >= -50 -> "Excelente"
            rssi >= -70 -> "Buena"
            rssi >= -85 -> "Regular"
            else -> "Débil"
        }
    }

    fun timeSinceLastSeen(): String {
        val seconds = (System.currentTimeMillis() - lastSeen) / 1000
        return when {
            seconds < 5 -> "Ahora"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h"
        }
    }
}
