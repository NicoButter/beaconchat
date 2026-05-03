package com.nicobutter.beaconchat.domain

/**
 * Represents WHAT the user wants to communicate — the semantic intent.
 *
 * This is the domain concept, completely decoupled from how the signal is sent.
 * The transmission channel is decided separately by [EmergencyMode] and the emitters.
 *
 * [morseMessage] is the English text encoded into Morse — keeps it universal.
 * [colorArgb] stores the ARGB value as a Long so this class has no Compose dependency.
 */
enum class EmergencyType(
    val displayName: String,
    val morseMessage: String,
    val colorArgb: Long,
    val icon: String
) {
    SOS("SOS", "SOS", 0xFFD32F2FL, "🆘"),
    HELP("AUXILIO", "HELP", 0xFFFF6F00L, "⚠️"),
    TRAPPED("ATRAPADO", "TRAPPED", 0xFFE65100L, "🚨"),
    KIDNAPPED("SECUESTRADO", "HELP", 0xFF6A1B9AL, "🔴"),
    INJURED("HERIDO", "INJURED", 0xFFBF360CL, "🩹"),
    OK("TODO BIEN", "OK", 0xFF388E3CL, "✅"),
    LOCATION("MI UBICACIÓN", "HERE", 0xFF1976D2L, "📍")
}
