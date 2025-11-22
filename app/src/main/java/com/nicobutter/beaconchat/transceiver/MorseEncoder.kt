package com.nicobutter.beaconchat.transceiver

class MorseEncoder {
    companion object {
        private const val DOT_DURATION = 200L // ms
        private const val DASH_DURATION = DOT_DURATION * 3
        private const val SYMBOL_SPACE = DOT_DURATION // Space between dots/dashes
        private const val LETTER_SPACE = DOT_DURATION * 3 // Space between letters
        private const val WORD_SPACE = DOT_DURATION * 7 // Space between words

        private val morseCodeMap = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
            'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
            '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
        )
    }

    /**
     * Encodes a string into a list of durations (in milliseconds).
     * The sequence alternates between ON and OFF states, starting with ON.
     * If the first element is 0, it means start with OFF (e.g. space).
     * However, for simplicity, we'll return a list of Pulse objects or just durations assuming ON/OFF toggle.
     *
     * Let's return a List<Long> where even indices (0, 2, 4...) are ON durations
     * and odd indices (1, 3, 5...) are OFF durations.
     */
    fun encode(text: String): List<Long> {
        val timings = mutableListOf<Long>()
        val upperText = text.uppercase()

        for (i in upperText.indices) {
            val char = upperText[i]

            if (char == ' ') {
                // Word space.
                // If we just finished a letter, we added a LETTER_SPACE (3 units).
                // A word space is 7 units. So we need to extend the last OFF duration.
                if (timings.isNotEmpty()) {
                    val lastOffIndex = timings.lastIndex
                    // The last entry is an OFF duration (inter-letter gap)
                    // We want to replace it with a WORD_SPACE
                    // But wait, standard says:
                    // Element space: 1 unit (between parts of same letter)
                    // Letter space: 3 units (between letters)
                    // Word space: 7 units (between words)
                    
                    // If we are appending, the last thing added was a letter space (3 units OFF).
                    // We should upgrade that to 7 units.
                    timings[lastOffIndex] = WORD_SPACE
                }
                continue
            }

            val code = morseCodeMap[char] ?: continue

            for (j in code.indices) {
                val symbol = code[j]
                
                // ON duration
                if (symbol == '.') {
                    timings.add(DOT_DURATION)
                } else {
                    timings.add(DASH_DURATION)
                }

                // OFF duration (Inter-element or Inter-letter)
                if (j < code.length - 1) {
                    timings.add(SYMBOL_SPACE)
                } else {
                    // End of letter, add Letter Space
                    // Unless it's the very last character of the message, but adding a trailing silence is fine.
                    timings.add(LETTER_SPACE)
                }
            }
        }
        return timings
    }
}
