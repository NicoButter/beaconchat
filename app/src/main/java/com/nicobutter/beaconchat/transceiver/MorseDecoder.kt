package com.nicobutter.beaconchat.transceiver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MorseDecoder {

    var decodedMessage by mutableStateOf("")
        private set

    private var lastChangeTime = 0L
    private var currentMessage = StringBuilder()
    private var currentSymbol = StringBuilder() // . or -

    // Standard timings (approximate, will need tolerance)
    // Reduced DOT_DURATION to match encoder change (120ms) for faster, reliable transmission
    private val DOT_DURATION = 120L
    private val DASH_DURATION = DOT_DURATION * 3
    private val SYMBOL_SPACE = DOT_DURATION
    private val LETTER_SPACE = DOT_DURATION * 3
    private val WORD_SPACE = DOT_DURATION * 7

    // Tolerance around measured durations. Reduced to accommodate the shorter DOT.
    private val tolerance = 60L // +/- 60ms

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

    fun onLightStateChanged(isLightOn: Boolean) {
        val now = System.currentTimeMillis()
        if (lastChangeTime == 0L) {
            lastChangeTime = now
            return
        }

        val duration = now - lastChangeTime
        lastChangeTime = now

        if (isLightOn) {
            // The light just turned ON, meaning it was OFF for 'duration'
            processOffDuration(duration)
        } else {
            // The light just turned OFF, meaning it was ON for 'duration'
            processOnDuration(duration)
        }
    }

    private fun processOnDuration(duration: Long) {
        // Threshold between Dot (1 unit) and Dash (3 units) is 2 units.
        // 2 units = DOT_DURATION * 2 = 400ms
        val threshold = DOT_DURATION * 2

        if (duration < threshold) {
            currentSymbol.append(".")
        } else {
            currentSymbol.append("-")
        }
    }

    private fun processOffDuration(duration: Long) {
        if (isCloseTo(duration, SYMBOL_SPACE)) {
            // Just a gap between parts of a letter, do nothing
        } else if (isCloseTo(duration, LETTER_SPACE)) {
            // End of a letter
            decodeCurrentSymbol()
        } else if (duration >= WORD_SPACE - tolerance) {
            // End of a word
            decodeCurrentSymbol()
            currentMessage.append(" ")
            decodedMessage = currentMessage.toString()
        }
    }

    private fun decodeCurrentSymbol() {
        if (currentSymbol.isNotEmpty()) {
            val char = morseCodeMapReverse[currentSymbol.toString()]
            if (char != null) {
                currentMessage.append(char)
                decodedMessage = currentMessage.toString()
            }
            currentSymbol.clear()
        }
    }

    private fun isCloseTo(value: Long, target: Long): Boolean {
        return Math.abs(value - target) <= tolerance
    }

    fun reset() {
        currentMessage.clear()
        currentSymbol.clear()
        lastChangeTime = 0L
        decodedMessage = ""
    }
}
