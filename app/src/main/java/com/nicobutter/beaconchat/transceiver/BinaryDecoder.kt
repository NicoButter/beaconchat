package com.nicobutter.beaconchat.transceiver

/**
 * Decodes binary-encoded ASCII text from individual bit streams.
 *
 * This class accumulates bits until it has enough to form complete ASCII characters
 * (8 bits per character). It provides methods to process individual bits and
 * retrieve decoded text.
 */
class BinaryDecoder {
    private val bitBuffer = mutableListOf<Char>()
    private var decodedText = StringBuilder()

    companion object {
        private const val BITS_PER_BYTE = 8
    }

    /**
     * Processes a single bit and attempts to decode a complete ASCII character.
     *
     * Accumulates bits in a buffer until 8 bits are collected, then converts
     * the binary string to an ASCII character. Only accepts '0' and '1' characters.
     *
     * @param bit The bit to process ('0' or '1')
     * @return The decoded ASCII character if a complete byte was formed, null otherwise
     */
    fun decodeBit(bit: Char): Char? {
        if (bit != '0' && bit != '1') return null

        bitBuffer.add(bit)

        // When we have 8 bits, decode the byte
        if (bitBuffer.size >= BITS_PER_BYTE) {
            val binaryString = bitBuffer.take(BITS_PER_BYTE).joinToString("")
            bitBuffer.clear()

            // Convert binary to decimal (ASCII value)
            val asciiValue = binaryString.toIntOrNull(2) ?: return null

            // Validate that it's a valid ASCII character
            if (asciiValue in 0..127) {
                return asciiValue.toChar()
            }
        }

        return null
    }

    /**
     * Resets the bit buffer and clears all decoded text.
     */
    fun reset() {
        bitBuffer.clear()
        decodedText.clear()
    }

    /**
     * Returns the accumulated decoded text.
     *
     * @return The complete decoded text string
     */
    fun getDecodedText(): String {
        return decodedText.toString()
    }

    /**
     * Adds a character to the accumulated decoded text.
     *
     * @param char The character to add to the decoded text
     */
    fun addCharacter(char: Char) {
        decodedText.append(char)
    }
}
