package com.nicobutter.beaconchat.transceiver

import java.util.Locale

/**
 * Encodes text into Morse code timing sequences for transmission.
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
 * ### Marcador de Inicio:
 * ```
 * ON 300ms → OFF 300ms → ON 900ms → OFF 500ms
 * ```
 * Patrón único que permite al receptor:
 * - Sincronizarse con el transmisor
 * - Diferenciar el mensaje de ruido ambiental
 * - Establecer niveles de referencia de intensidad
 *
 * ### Marcador de Fin:
 * ```
 * OFF 600ms → ON 100ms
 * ```
 * Señala el término del mensaje y permite:
 * - Detección de mensajes incompletos
 * - Concatenación de múltiples mensajes
 *
 * ## Tiempos del Protocolo
 *
 * Optimizados para cámaras a 30fps (~33ms/frame):
 * - **DOT**: 150ms (~4-5 frames) - Visible y distinguible
 * - **DASH**: 400ms (~12 frames) - Claramente más largo que DOT
 * - **Espacio símbolo**: 150ms - Separa . y - dentro de una letra
 * - **Espacio letra**: 500ms - Separa letras
 * - **Espacio palabra**: 1000ms - Separa palabras
 *
 * ## Ejemplo de Transmisión
 *
 * Texto: "SOS"
 * ```
 * [INICIO: 300ON-300OFF-900ON-500OFF]
 * S: 150ON-150OFF-150ON-150OFF-150ON-500OFF
 * O: 400ON-150OFF-400ON-150OFF-400ON-500OFF
 * S: 150ON-150OFF-150ON-150OFF-150ON-600OFF
 * [FIN: 100ON]
 * ```
 * Duración total: ~6.5 segundos
 *
 * ## Referencias
 * - IEEE 802.15.7 (Visible Light Communication)
 * - NIST Search and Rescue Standards
 * - ITU-R M.1677 (Morse Code Standard)
 *
 * @see PROTOCOLO_OPTICO.md para especificación completa
 */
class MorseEncoder(private val locale: Locale = Locale.getDefault()) {
    
    // Obtener el mapa Morse según el idioma del sistema
    private val morseCodeMap = MorseAlphabet.getMorseMap(locale)
    
    /**
     * Gets the name of the Morse alphabet being used.
     * Useful for UI display.
     */
    fun getAlphabetName(): String = MorseAlphabet.getAlphabetName(locale)
    
    /**
     * Gets the flag emoji for the current alphabet/locale.
     * Useful for visual identification of the encoding language.
     */
    fun getFlag(): String = MorseAlphabet.getFlag(locale)
    companion object {
        // Protocolo optimizado para comunicación óptica con cámara
        // Basado en estándares de robótica de rescate y beacons ópticos
        
        // Tiempos base del protocolo
        private const val DOT_DURATION = 150L // ms - Punto
        private const val DASH_DURATION = 400L // ms - Raya (2.66x DOT)
        private const val SYMBOL_SPACE = 150L // ms - Entre símbolos de una letra
        private const val LETTER_SPACE = 500L // ms - Entre letras
        private const val WORD_SPACE = 1000L // ms - Entre palabras
        
        // Marcadores del protocolo extendido
        private const val START_MARKER_ON_1 = 300L // Primer pulso ON
        private const val START_MARKER_OFF = 300L // Espacio
        private const val START_MARKER_ON_2 = 900L // Segundo pulso ON (largo)
        private const val END_MARKER_OFF = 600L // Espacio final
        private const val END_MARKER_ON = 100L // Pulso de fin corto
    }

    /** 
     * Converts text to its Morse code representation (human-readable string).
     * Automatically uses the correct alphabet for the system locale.
     */
    fun encodeToString(text: String): String {
        return text
                .map { char ->
                    if (char == ' ' || char.isWhitespace()) {
                        " / " // Separador de palabras
                    } else {
                        morseCodeMap[char] ?: morseCodeMap[char.uppercaseChar()] ?: ""
                    }
                }
                .joinToString(" ")
    }    /**
     * Encodes text into a list of timing durations for transmission with protocol markers.
     *
     * Implementa el protocolo extendido con:
     * - Marcador de inicio: ON 300 → OFF 300 → ON 900
     * - Mensaje en Morse
     * - Marcador de fin: OFF 600 → ON 100
     *
     * Returns a List<Long> where even indices (0, 2, 4...) are ON durations
     * and odd indices (1, 3, 5...) are OFF durations.
     *
     * @param text The text to encode into Morse code timing sequence
     * @return List of durations in milliseconds, alternating ON/OFF periods
     */
    fun encode(text: String): List<Long> {
        val timings = mutableListOf<Long>()
        
        // ========== MARCADOR DE INICIO ==========
        // ON 300 → OFF 300 → ON 900
        timings.add(START_MARKER_ON_1) // ON 300ms
        timings.add(START_MARKER_OFF)  // OFF 300ms
        timings.add(START_MARKER_ON_2) // ON 900ms
        timings.add(LETTER_SPACE)       // OFF 500ms (separación antes del mensaje)
        
        // ========== MENSAJE MORSE ==========
        val upperText = text.uppercase(locale)

        for (i in upperText.indices) {
            val char = upperText[i]

            if (char == ' ' || char.isWhitespace()) {
                // Espacio entre palabras
                if (timings.isNotEmpty()) {
                    val lastOffIndex = timings.lastIndex
                    // Extender el último OFF a WORD_SPACE
                    timings[lastOffIndex] = WORD_SPACE
                }
                continue
            }

            val code = morseCodeMap[char] ?: continue

            for (j in code.indices) {
                val symbol = code[j]

                // ON duration
                if (symbol == '.') {
                    timings.add(DOT_DURATION) // 150ms
                } else {
                    timings.add(DASH_DURATION) // 400ms
                }

                // OFF duration
                if (j < code.length - 1) {
                    // Entre símbolos de la misma letra
                    timings.add(SYMBOL_SPACE) // 150ms
                } else {
                    // Fin de letra
                    timings.add(LETTER_SPACE) // 500ms
                }
            }
        }
        
        // ========== MARCADOR DE FIN ==========
        // Reemplazar el último OFF por END_MARKER_OFF
        if (timings.isNotEmpty()) {
            timings[timings.lastIndex] = END_MARKER_OFF // OFF 600ms
        }
        timings.add(END_MARKER_ON) // ON 100ms (pulso final corto)
        
        return timings
    }
}
