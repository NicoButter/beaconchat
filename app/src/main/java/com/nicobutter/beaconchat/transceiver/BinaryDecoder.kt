package com.nicobutter.beaconchat.transceiver

class BinaryDecoder {
    private val bitBuffer = mutableListOf<Char>()
    private var decodedText = StringBuilder()
    
    companion object {
        private const val BITS_PER_BYTE = 8
    }

    /**
     * Procesa una secuencia de bits (0s y 1s) y decodifica a texto ASCII.
     * @param bit El bit detectado ('0' o '1')
     * @return El carácter decodificado o null si aún no se completa un byte
     */
    fun decodeBit(bit: Char): Char? {
        if (bit != '0' && bit != '1') return null
        
        bitBuffer.add(bit)
        
        // Cuando tenemos 8 bits, decodificar el byte
        if (bitBuffer.size >= BITS_PER_BYTE) {
            val binaryString = bitBuffer.take(BITS_PER_BYTE).joinToString("")
            bitBuffer.clear()
            
            // Convertir binario a decimal (valor ASCII)
            val asciiValue = binaryString.toIntOrNull(2) ?: return null
            
            // Validar que sea un carácter ASCII válido
            if (asciiValue in 0..127) {
                return asciiValue.toChar()
            }
        }
        
        return null
    }

    /**
     * Reinicia el buffer de bits y el texto decodificado
     */
    fun reset() {
        bitBuffer.clear()
        decodedText.clear()
    }

    /**
     * Obtiene el texto decodificado acumulado
     */
    fun getDecodedText(): String {
        return decodedText.toString()
    }

    /**
     * Añade un carácter al texto decodificado
     */
    fun addCharacter(char: Char) {
        decodedText.append(char)
    }
}
