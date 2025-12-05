package com.nicobutter.beaconchat.transceiver

/**
 * Encodes text into binary ASCII representation for transmission via light/sound signals.
 *
 * This class converts text characters to their 8-bit ASCII binary representation and
 * generates timing sequences suitable for transmission through various mediums like
 * flashlight, sound, or vibration.
 */
class BinaryEncoder {
    companion object {
        private const val BIT_DURATION = 200L // ms - duration of each bit (0 or 1)
        private const val BYTE_SPACE = 400L // ms - pause between bytes (characters)
        private const val WORD_SPACE = 800L // ms - pause between words
    }

    /**
     * Converts text to its ASCII binary representation as a readable string.
     *
     * @param text The text to encode
     * @return Binary representation with spaces between bytes and "/" for word separators
     */
    fun encodeToString(text: String): String {
        return text
                .map { char ->
                    if (char == ' ') {
                        " / " // Word separator
                    } else {
                        val asciiValue = char.code
                        asciiValue.toString(2).padStart(8, '0')
                    }
                }
                .joinToString(" ")
    }

    /**
     * Encodes text into ASCII binary format for transmission.
     *
     * Each character is converted to its 8-bit ASCII value. A '1' is represented
     * with light/sound ON, a '0' with light/sound OFF.
     *
     * @param text The text to encode
     * @return List of durations alternating between ON and OFF periods
     */
    fun encode(text: String): List<Long> {
        val timings = mutableListOf<Long>()

        for (i in text.indices) {
            val char = text[i]

            if (char == ' ') {
                // Space between words - add extra pause
                if (timings.isNotEmpty() && timings.last() != WORD_SPACE) {
                    // If last was ON, add long OFF
                    timings.add(WORD_SPACE)
                }
                continue
            }

            // Convert character to 8-bit binary
            val asciiValue = char.code // Get ASCII value
            val binaryString = asciiValue.toString(2).padStart(8, '0')

            // Transmit each bit
            for (bit in binaryString) {
                if (bit == '1') {
                    // Bit 1: light/sound ON
                    timings.add(BIT_DURATION)
                    timings.add(BIT_DURATION) // OFF between bits
                } else {
                    // Bit 0: light/sound OFF (pause)
                    // To maintain rhythm, add a pause of bit duration
                    if (timings.isNotEmpty()) {
                        // Extend the last pause
                        val lastIndex = timings.lastIndex
                        if (lastIndex % 2 == 1) { // If it's a pause (odd index)
                            timings[lastIndex] = timings[lastIndex] + BIT_DURATION
                        } else {
                            timings.add(BIT_DURATION)
                        }
                    } else {
                        timings.add(0L) // Start with OFF
                        timings.add(BIT_DURATION)
                    }
                }
            }

            // Add pause between bytes (characters)
            if (i < text.length - 1) {
                if (timings.isNotEmpty() && timings.lastIndex % 2 == 0) {
                    // If ended on ON, add OFF
                    timings.add(BYTE_SPACE)
                } else if (timings.isNotEmpty()) {
                    // If ended on OFF, extend
                    timings[timings.lastIndex] = timings[timings.lastIndex] + BYTE_SPACE
                }
            }
        }

        return timings
    }
}
