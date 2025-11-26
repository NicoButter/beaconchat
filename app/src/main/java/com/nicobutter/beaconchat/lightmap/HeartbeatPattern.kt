package com.nicobutter.beaconchat.lightmap

/**
 * HeartbeatPattern - Define el patrón luminoso que identifica a un dispositivo BeaconChat
 * 
 * Este patrón se transmite periódicamente para permitir que otros dispositivos
 * detecten la presencia y posición aproximada del transmisor.
 */
object HeartbeatPattern {
    
    // Patrón básico: 3 pulsos cortos rápidos (similar a SOS simplificado)
    // Esto es reconocible y breve (~500ms total)
    private const val SHORT_PULSE = 80L  // ms
    private const val SHORT_GAP = 80L    // ms
    private const val PATTERN_GAP = 200L // ms entre repeticiones
    
    /**
     * Genera el patrón de heartbeat como lista de duraciones (ON, OFF, ON, OFF...)
     * 
     * Patrón: • • • (3 pulsos cortos)
     */
    fun generateHeartbeat(): List<Long> {
        return listOf(
            SHORT_PULSE,  // ON
            SHORT_GAP,    // OFF
            SHORT_PULSE,  // ON
            SHORT_GAP,    // OFF
            SHORT_PULSE,  // ON
            PATTERN_GAP   // OFF (pausa final)
        )
    }
    
    /**
     * Genera un heartbeat con ID único (variando los intervalos ligeramente)
     * Esto permite diferenciar dispositivos en el futuro
     */
    fun generateHeartbeatWithId(deviceId: String): List<Long> {
        // Por ahora usa el patrón estándar
        // En el futuro podría modular los intervalos según el ID
        return generateHeartbeat()
    }
    
    /**
     * Duración total del patrón de heartbeat en milisegundos
     */
    fun getPatternDuration(): Long {
        return generateHeartbeat().sum()
    }
    
    /**
     * Intervalo recomendado entre transmisiones de heartbeat (en ms)
     * 3 segundos para no saturar
     */
    const val HEARTBEAT_INTERVAL = 3000L
    
    /**
     * Patrón de emergencia (más agresivo): SOS en Morse
     * • • •  — — —  • • •
     */
    fun generateSOSPattern(): List<Long> {
        val dot = 100L
        val dash = 300L
        val gap = 100L
        val letterGap = 300L
        
        return listOf(
            // S (• • •)
            dot, gap, dot, gap, dot, letterGap,
            // O (— — —)
            dash, gap, dash, gap, dash, letterGap,
            // S (• • •)
            dot, gap, dot, gap, dot, letterGap * 2
        )
    }
}
