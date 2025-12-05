package com.nicobutter.beaconchat.mesh

/**
 * Represents a peer device discovered in the BeaconChat mesh network.
 *
 * This data class contains information about other BeaconChat devices that have been
 * discovered through Bluetooth LE scanning. It includes device identification, signal
 * strength, and timing information to help determine device availability and proximity.
 *
 * @property id Unique identifier (Bluetooth MAC address) of the peer device
 * @property name Bluetooth device name as reported by the device
 * @property callsign User-defined callsign broadcast by the device
 * @property rssi Received Signal Strength Indicator in dBm (-127 to 20)
 * @property lastSeen Timestamp when this device was last detected (milliseconds since epoch)
 */
data class MeshPeer(
        val id: String, // Bluetooth address or device ID
        val name: String, // Device name
        val callsign: String, // User callsign
        val rssi: Int, // Signal strength (dBm)
        val lastSeen: Long // Timestamp in milliseconds
) {
    /**
     * Returns a human-readable signal quality description based on RSSI.
     *
     * @return Signal quality: "Excellent", "Good", "Fair", or "Weak"
     */
    fun getSignalQuality(): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -70 -> "Good"
            rssi >= -85 -> "Fair"
            else -> "Weak"
        }
    }

    /**
     * Returns a human-readable time string since the device was last seen.
     *
     * @return Time string in format like "Now", "5s", "2m", or "1h"
     */
    fun timeSinceLastSeen(): String {
        val seconds = (System.currentTimeMillis() - lastSeen) / 1000
        return when {
            seconds < 5 -> "Now"
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            else -> "${seconds / 3600}h"
        }
    }
}
