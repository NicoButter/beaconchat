package com.nicobutter.beaconchat.transceiver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Decodes Morse code signals from light state changes.
 *
 * This class analyzes sequences of light on/off transitions to decode Morse code
 * messages. It uses timing analysis to distinguish between dots, dashes, and
 * various pause durations that separate symbols, letters, and words.
 */
class MorseDecoder {

    /** The currently decoded message, updated reactively for UI binding. */
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

    /**
     * Processes a light state change event for Morse code decoding.
     *
     * Call this method whenever the light detector detects a transition between
     * light on and light off states. The decoder uses timing analysis to interpret
     * the Morse code sequence.
     *
     * @param isLightOn The current light state (true = on, false = off)
     */
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

    /**
     * Processes the duration of an ON period (light was on).
     *
     * Determines whether the duration represents a dot or dash based on
     * the threshold between DOT_DURATION * 2.
     *
     * @param duration The duration the light was on in milliseconds
     */
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

    /**
     * Processes the duration of an OFF period (light was off).
     *
     * Interprets pauses as symbol separators, letter separators, or word separators
     * based on their duration relative to standard Morse timing.
     *
     * @param duration The duration the light was off in milliseconds
     */
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

    /**
     * Decodes the current accumulated symbol and adds it to the message.
     *
     * Looks up the current symbol in the Morse code map and appends the
     * corresponding character to the message if found.
     */
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

    /**
     * Checks if a duration value is close to a target value within tolerance.
     *
     * @param value The measured duration
     * @param target The expected duration
     * @return true if the value is within tolerance of the target
     */
    private fun isCloseTo(value: Long, target: Long): Boolean {
        return Math.abs(value - target) <= tolerance
    }

    /**
     * Resets the decoder state, clearing all buffers and the decoded message.
     */
    fun reset() {
        currentMessage.clear()
        currentSymbol.clear()
        lastChangeTime = 0L
        decodedMessage = ""
    }
}
