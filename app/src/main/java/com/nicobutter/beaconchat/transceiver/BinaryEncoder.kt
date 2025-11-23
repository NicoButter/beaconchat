package com.nicobutter.beaconchat.transceiver

class BinaryEncoder {
    companion object {
        private const val BIT_DURATION = 200L // ms - duración de cada bit (0 o 1)
        private const val BYTE_SPACE = 400L // ms - pausa entre bytes (caracteres)
        private const val WORD_SPACE = 800L // ms - pausa entre palabras
    }

    /**
     * Codifica un texto en ASCII binario.
     * Cada carácter se convierte a su valor ASCII de 8 bits.
     * Un '1' se representa con luz encendida, un '0' con luz apagada.
     * 
     * Retorna una lista de duraciones alternas (ON, OFF, ON, OFF...)
     */
    fun encode(text: String): List<Long> {
        val timings = mutableListOf<Long>()
        
        for (i in text.indices) {
            val char = text[i]
            
            if (char == ' ') {
                // Espacio entre palabras - añadir pausa extra
                if (timings.isNotEmpty() && timings.last() != WORD_SPACE) {
                    // Si el último fue un ON, añadir OFF largo
                    timings.add(WORD_SPACE)
                }
                continue
            }
            
            // Convertir carácter a binario de 8 bits
            val asciiValue = char.code // Obtiene el valor ASCII
            val binaryString = asciiValue.toString(2).padStart(8, '0')
            
            // Transmitir cada bit
            for (bit in binaryString) {
                if (bit == '1') {
                    // Bit 1: luz encendida
                    timings.add(BIT_DURATION)
                    timings.add(BIT_DURATION) // OFF entre bits
                } else {
                    // Bit 0: luz apagada (pausa)
                    // Para mantener el ritmo, añadimos una pausa de duración de bit
                    if (timings.isNotEmpty()) {
                        // Extender la última pausa
                        val lastIndex = timings.lastIndex
                        if (lastIndex % 2 == 1) { // Si es una pausa (índice impar)
                            timings[lastIndex] = timings[lastIndex] + BIT_DURATION
                        } else {
                            timings.add(BIT_DURATION)
                        }
                    } else {
                        timings.add(0L) // Empezar con OFF
                        timings.add(BIT_DURATION)
                    }
                }
            }
            
            // Añadir pausa entre bytes (caracteres)
            if (i < text.length - 1) {
                if (timings.isNotEmpty() && timings.lastIndex % 2 == 0) {
                    // Si terminamos en ON, añadir OFF
                    timings.add(BYTE_SPACE)
                } else if (timings.isNotEmpty()) {
                    // Si terminamos en OFF, extender
                    timings[timings.lastIndex] = timings[timings.lastIndex] + BYTE_SPACE
                }
            }
        }
        
        return timings
    }
}
