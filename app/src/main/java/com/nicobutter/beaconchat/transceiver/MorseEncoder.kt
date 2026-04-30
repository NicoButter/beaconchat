package com.nicobutter.beaconchat.transceiver

import java.util.Locale

/**
 * Encodes text to Morse code timing sequences for optical transmission.
 *
 * Implementa el protocolo estándar de comunicación óptica usado en robótica de rescate,
 * drones autónomos y beacons de navegación. El protocolo incluye marcadores de inicio/fin
 * para sincronización robusta y tiempos optimizados para detección con cámara.
 *
 * ## Soporte Multi-Idioma
 *
 * Detecta automáticamente el idioma del sistema y usa el alfabeto Morse correspondiente:
 * - **Latino** (International Standard - ITU-R M.1677)
 * - **Cirílico** (Ruso, Ucraniano, Bielorruso, Búlgaro, Serbio)
 * - **Griego** (Ελληνικά)
 * - **Hebreo** (עברית)
 * - **Árabe** (العربية)
 * - **Japonés** (Wabun code - かな)
 * - **Coreano** (Hangul - 한글)
 * - **Tailandés** (ภาษาไทย)
 * - **Persa** (فارسی)
 *
 * Ejemplo:
 * ```kotlin
 * val encoder = MorseEncoder()
 * // En sistema en español: usa alfabeto latino
 * val timings = encoder.encode("HOLA")
 * // En sistema en ruso: usa alfabeto cirílico
 * val timingsRu = encoder.encode("ПРИВЕТ")
 * ```
 *
 * ## Protocolo Extendido
 *
 * ### Estructura del mensaje:
 * ```
 * [MARCADOR INICIO] → [MENSAJE MORSE] → [MARCADOR FIN]
 * ```
 *
 * ### Preámbulo de Sincronización:
 * ```
 * ON 800ms → OFF 400ms → ON 800ms → OFF 800ms
 * ```
 * Patrón LARGO-CORTO-LARGO que permite al receptor:
 * - Sincronizarse con el transmisor
 * - Calibrar umbrales de detección (luz ON vs OFF)
 * - Diferenciar el mensaje de ruido ambiental
 * - Establecer niveles de referencia de intensidad
 * 
 * Simple, synchronized protocol:
 * - DOT: 150ms
 * - DASH: 400ms
 * - Symbol gap: 150ms
 * - Letter gap: 500ms
 * - Word gap: 1000ms
 * - Preamble: ON 300, OFF 300, ON 900, OFF 500
 */
class MorseEncoder(private val locale: Locale = Locale.getDefault()) {

    private val morseCodeMap = MorseAlphabet.getMorseMap(locale)
    
    fun getFlag(): String = MorseAlphabet.getFlag(locale)
    fun getAlphabetName(): String = MorseAlphabet.getAlphabetName(locale)

    companion object {
        // === MORSE TIMING (exact values from PROTOCOLO_OPTICO.md) ===
        const val DOT_DURATION = 150L       // Punto: 150ms ON
        const val DASH_DURATION = 400L      // Raya: 400ms ON
        const val SYMBOL_SPACE = 150L       // Entre símbolos: 150ms OFF
        const val LETTER_SPACE = 500L       // Entre letras: 500ms OFF
        const val WORD_SPACE = 1000L        // Entre palabras: 1000ms OFF

        // === PREAMBLE (exact values from protocol) ===
        // Patrón único: ON 300ms → OFF 300ms → ON 900ms → OFF 500ms
        const val PREAMBLE_ON_1 = 300L      // Primera luz: 300ms ON
        const val PREAMBLE_OFF_1 = 300L     // Apagado: 300ms OFF
        const val PREAMBLE_ON_2 = 900L      // Segunda luz: 900ms ON
        const val PREAMBLE_OFF_2 = 500L     // Apagado: 500ms OFF
        
        // === END MARKER ===
        const val END_OFF = 600L            // Silencio final: 600ms OFF
        const val END_ON = 100L             // Pulso final: 100ms ON
    }

    /**
     * Encode text to timing sequence. 
     * 
     * Returns: [ON1, OFF1, ON2, OFF2, ...]
     * - Even indices (0,2,4...) = ON durations
     * - Odd indices (1,3,5...) = OFF durations
     * 
     * Structure: PREAMBLE + MESSAGE + END_MARKER
     */
    fun encode(text: String): List<Long> {
        val timings = mutableListOf<Long>()

        // === PREAMBLE (sincronización) ===
        timings.add(PREAMBLE_ON_1)    // [0] ON 300ms
        timings.add(PREAMBLE_OFF_1)   // [1] OFF 300ms
        timings.add(PREAMBLE_ON_2)    // [2] ON 900ms
        timings.add(PREAMBLE_OFF_2)   // [3] OFF 500ms

        // === MESSAGE ===
        val upperText = text.uppercase(locale)

        for (char in upperText) {
            if (char == ' ' || char.isWhitespace()) {
                // Word gap
                if (timings.size > 1) {
                    timings[timings.lastIndex] = WORD_SPACE
                }
                continue
            }

            val morse = morseCodeMap[char] ?: continue

            for ((idx, symbol) in morse.withIndex()) {
                // ON duration
                if (symbol == '.') {
                    timings.add(DOT_DURATION)
                } else {
                    timings.add(DASH_DURATION)
                }

                // OFF duration
                if (idx < morse.length - 1) {
                    timings.add(SYMBOL_SPACE)
                } else {
                    timings.add(LETTER_SPACE)
                }
            }
        }

        // === END MARKER (marcador de fin) ===
        if (timings.size > 1) {
            timings[timings.lastIndex] = END_OFF  // Reemplazar último OFF con 1000ms
        }
        timings.add(END_ON)  // Pulso final 200ms ON

        android.util.Log.d("MorseEncoder", "Encoded '$text' → ${timings.size} timings: [${timings.take(10).joinToString()}...]")
        return timings
    }
}
