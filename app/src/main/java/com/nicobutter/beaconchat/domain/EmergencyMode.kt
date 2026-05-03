package com.nicobutter.beaconchat.domain

/**
 * Represents HOW the emergency signal is transmitted.
 *
 * Completely separated from [EmergencyType] (what is communicated).
 * This lets the UI just say: "send SOS in DISCREET mode" — no channel logic needed.
 *
 * [DISCREET] uses BLE only: no visible light, no sound, no vibration.
 * Useful when the victim cannot attract attention openly.
 */
enum class EmergencyMode(
    val displayName: String,
    val icon: String,
    val description: String
) {
    ALL("Todo", "🌟", "Linterna + vibración + sonido + BLE"),
    LIGHT("Luz", "💡", "Solo linterna"),
    VIBRATION("Vibración", "📳", "Solo vibración"),
    SOUND("Ultrasonido", "🔊", "Solo ultrasonido"),
    BLE("Bluetooth", "📡", "Solo BLE"),
    DISCREET("Discreto", "🤫", "Solo BLE — sin luz ni ruido")
}
