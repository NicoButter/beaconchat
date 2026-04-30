package com.nicobutter.beaconchat.transceiver

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Decodes Morse code from measured on/off durations (milliseconds).
 * 
 * IMPORTANTE: onLightStateChanged recibe:
 * - isLightOn: NUEVO estado (true=acaba de encenderse, false=acaba de apagarse)
 * - durationMs: duración del estado que ACABA DE TERMINAR
 * 
 * Rangos de clasificación (±50ms para DOT, ±150ms para DASH):
 * - DOT: 150-250ms (nominal 200ms)
 * - DASH: 450-750ms (nominal 600ms)
 * - Symbol gap: 150-250ms (nominal 200ms)
 * - Letter gap: 450-750ms (nominal 600ms)
 * - Word gap: 1200-1600ms (nominal 1400ms)
 * 
 * Preamble (±100ms):
 * - ON_1: 700-900ms (nominal 800ms)
 * - OFF_1: 300-500ms (nominal 400ms)
 * - ON_2: 700-900ms (nominal 800ms)
 * - OFF_2: 700-900ms (nominal 800ms)
 */
class MorseDecoder {

    var decodedMessage by mutableStateOf("")
        private set

    private var currentMessage = StringBuilder()
    private var currentSymbol = StringBuilder()
    private var messageStarted = false

    // === MORSE TIMING RANGES (ms) ===
    private companion object {
        // ON durations (símbolos)
        const val DOT_MIN = 150L
        const val DOT_MAX = 250L
        const val DASH_MIN = 450L
        const val DASH_MAX = 750L
        
        // OFF durations (gaps)
        const val SYMBOL_SPACE_MIN = 150L
        const val SYMBOL_SPACE_MAX = 250L
        const val LETTER_SPACE_MIN = 450L
        const val LETTER_SPACE_MAX = 750L
        const val WORD_SPACE_MIN = 1200L
        const val WORD_SPACE_MAX = 1600L
        
        // === PREAMBLE RANGES (ms) ===
        const val PREAMBLE_ON_1_MIN = 700L
        const val PREAMBLE_ON_1_MAX = 900L
        const val PREAMBLE_OFF_1_MIN = 300L
        const val PREAMBLE_OFF_1_MAX = 500L
        const val PREAMBLE_ON_2_MIN = 700L
        const val PREAMBLE_ON_2_MAX = 900L
        const val PREAMBLE_OFF_2_MIN = 700L
        const val PREAMBLE_OFF_2_MAX = 900L
    }
    
    private var preambleStage = 0  // 0=waiting ON_1, 1=got ON_1 waiting OFF_1, 2=got OFF_1 waiting ON_2, 3=got ON_2 waiting OFF_2

    private val morseCodeMapReverse =
            mapOf(
                    ".-" to 'A',
                    "-..." to 'B',
                    "-.-." to 'C',
                    "-.." to 'D',
                    "." to 'E',
                    "..-." to 'F',
                    "--." to 'G',
                    "...." to 'H',
                    ".." to 'I',
                    ".---" to 'J',
                    "-.-" to 'K',
                    ".-.." to 'L',
                    "--" to 'M',
                    "-." to 'N',
                    "---" to 'O',
                    ".--." to 'P',
                    "--.-" to 'Q',
                    ".-." to 'R',
                    "..." to 'S',
                    "-" to 'T',
                    "..-" to 'U',
                    "...-" to 'V',
                    ".--" to 'W',
                    "-..-" to 'X',
                    "-.--" to 'Y',
                    "--.." to 'Z',
                    "-----" to '0',
                    ".----" to '1',
                    "..---" to '2',
                    "...--" to '3',
                    "....-" to '4',
                    "....." to '5',
                    "-...." to '6',
                    "--..." to '7',
                    "---.." to '8',
                    "----." to '9'
            )

    /**
     * Procesa cambio de estado de luz.
     * 
     * IMPORTANTE: 
     * - isLightOn: NUEVO estado (true=SE ACABA DE ENCENDER, false=SE ACABA DE APAGAR)
     * - durationMs: duración del estado que ACABA DE TERMINAR
     * 
     * Ejemplo:
     * - LED estaba ON 800ms, luego se apaga → isLightOn=false, durationMs=800
     * - LED estaba OFF 400ms, luego se enciende → isLightOn=true, durationMs=400
     */
    fun onLightStateChanged(isLightOn: Boolean, durationMs: Long) {
        Log.d("MorseDecoder", "StateChange: ${if (isLightOn) "OFF→ON" else "ON→OFF"}, prevDuration=${durationMs}ms, stage=$preambleStage")
        
        // === PREAMBLE DETECTION ===
        if (!messageStarted) {
            when (preambleStage) {
                0 -> { // Esperando primera luz ON_1 (800ms)
                    // Cuando se APAGA después del primer ON
                    if (!isLightOn && inRange(durationMs, PREAMBLE_ON_1_MIN, PREAMBLE_ON_1_MAX)) {
                        preambleStage = 1
                        Log.w("MorseDecoder", "[1/4] ✓ ON_1 = ${durationMs}ms (esperado 700-900ms)")
                    } else if (!isLightOn) {
                        Log.d("MorseDecoder", "[1/4] ✗ ON duró ${durationMs}ms (esperado 700-900ms)")
                    }
                }
                1 -> { // Esperando OFF_1 (400ms)
                    // Cuando se ENCIENDE después del primer OFF
                    if (isLightOn && inRange(durationMs, PREAMBLE_OFF_1_MIN, PREAMBLE_OFF_1_MAX)) {
                        preambleStage = 2
                        Log.w("MorseDecoder", "[2/4] ✓ OFF_1 = ${durationMs}ms (esperado 300-500ms)")
                    } else if (isLightOn) {
                        Log.d("MorseDecoder", "[2/4] ✗ OFF duró ${durationMs}ms, RESET")
                        preambleStage = 0
                    }
                }
                2 -> { // Esperando segunda luz ON_2 (800ms)
                    // Cuando se APAGA después del segundo ON
                    if (!isLightOn && inRange(durationMs, PREAMBLE_ON_2_MIN, PREAMBLE_ON_2_MAX)) {
                        preambleStage = 3
                        Log.w("MorseDecoder", "[3/4] ✓ ON_2 = ${durationMs}ms (esperado 700-900ms)")
                    } else if (!isLightOn) {
                        Log.d("MorseDecoder", "[3/4] ✗ ON duró ${durationMs}ms, RESET")
                        preambleStage = 0
                    }
                }
                3 -> { // Esperando OFF_2 final (800ms)
                    // Cuando se ENCIENDE después del segundo OFF
                    if (isLightOn && inRange(durationMs, PREAMBLE_OFF_2_MIN, PREAMBLE_OFF_2_MAX)) {
                        messageStarted = true
                        preambleStage = 0
                        Log.w("MorseDecoder", "[4/4] ✓ OFF_2 = ${durationMs}ms → 🎉 PREAMBLE COMPLETO! Decodificando mensaje...")
                    } else if (isLightOn) {
                        Log.d("MorseDecoder", "[4/4] ✗ OFF duró ${durationMs}ms, RESET")
                        preambleStage = 0
                    }
                }
            }
            return
        }

        // === MESSAGE DECODING (después del preamble) ===
        // Cuando se APAGA: procesamos el ON que acaba de terminar (DOT/DASH)
        // Cuando se ENCIENDE: procesamos el OFF que acaba de terminar (gap)
        if (!isLightOn) {
            // LED se acaba de apagar → el ON anterior fue un símbolo
            processOnDuration(durationMs)
        } else {
            // LED se acaba de encender → el OFF anterior fue un gap
            processOffDuration(durationMs)
        }
    }

    private fun processOnDuration(durationMs: Long) {
        // Clasificar ON como DOT (punto) o DASH (raya)
        when {
            inRange(durationMs, DOT_MIN, DOT_MAX) -> {
                currentSymbol.append(".")
                Log.w("MorseDecoder", "· DOT (${durationMs}ms) → símbolo actual: $currentSymbol")
            }
            inRange(durationMs, DASH_MIN, DASH_MAX) -> {
                currentSymbol.append("-")
                Log.w("MorseDecoder", "━ DASH (${durationMs}ms) → símbolo actual: $currentSymbol")
            }
            else -> Log.d("MorseDecoder", "⚠ ON=${durationMs}ms fuera de rango (esperado DOT 150-250 o DASH 450-750)")
        }
    }

    private fun processOffDuration(durationMs: Long) {
        // Clasificar OFF como espacio entre símbolos, letras o palabras
        when {
            inRange(durationMs, SYMBOL_SPACE_MIN, SYMBOL_SPACE_MAX) -> {
                Log.d("MorseDecoder", "  GAP símbolo (${durationMs}ms) → continuar letra")
                // Continuar con el símbolo actual
            }
            inRange(durationMs, LETTER_SPACE_MIN, LETTER_SPACE_MAX) -> {
                Log.w("MorseDecoder", "   GAP letra (${durationMs}ms) → decodificar: $currentSymbol")
                decodeCurrentSymbol()
            }
            inRange(durationMs, WORD_SPACE_MIN, WORD_SPACE_MAX) -> {
                Log.w("MorseDecoder", "    GAP palabra (${durationMs}ms)")
                decodeCurrentSymbol()
                currentMessage.append(" ")
                decodedMessage = currentMessage.toString()
                Log.w("MorseDecoder", "📝 Mensaje: '$decodedMessage'")
            }
            else -> Log.d("MorseDecoder", "⚠ OFF=${durationMs}ms fuera de rango (esperado 150-250, 450-750 o 1200-1600)")
        }
    }

    private fun decodeCurrentSymbol() {
        if (currentSymbol.isNotEmpty()) {
            val symbolStr = currentSymbol.toString()
            val char = morseCodeMapReverse[symbolStr]
            if (char != null) {
                currentMessage.append(char)
                decodedMessage = currentMessage.toString()
                Log.w("MorseDecoder", "✓ '$symbolStr' → '$char' | Mensaje: '$decodedMessage'")
            } else {
                Log.w("MorseDecoder", "✗ Símbolo desconocido: '$symbolStr'")
            }
            currentSymbol.clear()
        }
    }

    private fun inRange(value: Long, min: Long, max: Long): Boolean {
        return value in min..max
    }

    fun reset() {
        currentMessage.clear()
        currentSymbol.clear()
        messageStarted = false
        preambleStage = 0
        decodedMessage = ""
    }
}
