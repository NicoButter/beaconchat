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
 * ### Marcador de Fin:
 * ```
 * OFF 1000ms → ON 200ms
 * ```
 * Señala el término del mensaje y permite:
 * - Detección de mensajes incompletos
 * - Concatenación de múltiples mensajes
 *
 * ## Tiempos del Protocolo
 *
 * Optimizados para cámaras a 30fps (33ms/frame):
 * - **DOT**: 200ms (6 frames) - Mínimo confiable para detección
 * - **DASH**: 600ms (18 frames) - Ratio 3:1 estándar Morse (ITU-R M.1677)
 * - **Espacio símbolo**: 200ms - Separa . y - dentro de una letra
 * - **Espacio letra**: 600ms (3 DOT) - Separa letras
 * - **Espacio palabra**: 1400ms (7 DOT) - Separa palabras
 *
 * ## Ejemplo de Transmisión
 *
 * Texto: "SOS"
 * ```
 * [INICIO: 800ON-400OFF-800ON-800OFF]
 * S: 200ON-200OFF-200ON-200OFF-200ON-600OFF
 * O: 600ON-200OFF-600ON-200OFF-600ON-600OFF
 * S: 200ON-200OFF-200ON-200OFF-200ON-1000OFF
 * [FIN: 200ON]
 * ```
 * Duración total: ~10.6 segundos
 * Total frames @ 30fps: ~318 frames
 *
 * ## Referencias
 * - IEEE 802.15.7 (Visible Light Communication)
 * - ITU-R M.1677 (International Morse Code Standard)
 * - NIST Search and Rescue Standards
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
        // Basado en IEEE 802.15.7 (Visible Light Communication) y ITU-R M.1677
        // Diseñado para cámaras a 30fps (33ms/frame)
        
        // Tiempos base del protocolo (mínimo 6 frames por DOT = 200ms)
        private const val DOT_DURATION = 200L // ms - Punto (6 frames @ 30fps)
        private const val DASH_DURATION = 600L // ms - Raya (3x DOT, estándar Morse)
        private const val SYMBOL_SPACE = 200L // ms - Entre símbolos de una letra
        private const val LETTER_SPACE = 600L // ms - Entre letras (3x DOT)
        private const val WORD_SPACE = 1400L // ms - Entre palabras (7x DOT)
        
        // Preámbulo de sincronización (IEEE 802.15.7 basado)
        // Patrón: LARGO-CORTO-LARGO para detección de inicio
        private const val START_MARKER_ON_1 = 800L // Primer pulso LARGO (marca inicio)
        private const val START_MARKER_OFF = 400L // Espacio MEDIO
        private const val START_MARKER_ON_2 = 800L // Segundo pulso LARGO (confirmación)
        private const val START_MARKER_OFF_2 = 800L // Espacio antes del mensaje
        
        // Marcador de fin (patrón único para detección)
        private const val END_MARKER_OFF = 1000L // Espacio final largo
        private const val END_MARKER_ON = 200L // Pulso de fin = 1 DOT
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
        
        // ========== PREÁMBULO DE SINCRONIZACIÓN ==========
        // Patrón: LARGO-CORTO-LARGO (IEEE 802.15.7 inspired)
        // ON 800 → OFF 400 → ON 800 → OFF 800
        timings.add(START_MARKER_ON_1)  // ON 800ms (LARGO)
        timings.add(START_MARKER_OFF)   // OFF 400ms (MEDIO)
        timings.add(START_MARKER_ON_2)  // ON 800ms (LARGO)
        timings.add(START_MARKER_OFF_2) // OFF 800ms (separación antes del mensaje)
        
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
                    timings.add(DOT_DURATION) // 200ms (6 frames @ 30fps)
                } else {
                    timings.add(DASH_DURATION) // 600ms (18 frames @ 30fps)
                }

                // OFF duration
                if (j < code.length - 1) {
                    // Entre símbolos de la misma letra
                    timings.add(SYMBOL_SPACE) // 200ms
                } else {
                    // Fin de letra
                    timings.add(LETTER_SPACE) // 600ms
                }
            }
        }
        
        // ========== MARCADOR DE FIN ==========
        // Reemplazar el último OFF por END_MARKER_OFF
        if (timings.isNotEmpty()) {
            timings[timings.lastIndex] = END_MARKER_OFF // OFF 1000ms
        }
        timings.add(END_MARKER_ON) // ON 200ms (1 DOT)
        
        return timings
    }
}
