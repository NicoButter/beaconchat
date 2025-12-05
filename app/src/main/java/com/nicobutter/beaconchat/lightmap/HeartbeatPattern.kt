package com.nicobutter.beaconchat.lightmap

/**
 * Defines the light pattern that identifies a BeaconChat device.
 *
 * This object contains the specifications for heartbeat patterns used by BeaconChat devices
 * to announce their presence to other devices. The patterns are designed to be recognizable,
 * brief, and suitable for optical detection through camera analysis.
 *
 * The basic pattern consists of three short pulses, similar to a simplified SOS signal,
 * transmitted periodically to allow other devices to detect presence and approximate position.
 */
object HeartbeatPattern {
    
    // Basic pattern: 3 short rapid pulses (similar to simplified SOS)
    // This is recognizable and brief (~500ms total)
    private const val SHORT_PULSE = 80L  // ms
    private const val SHORT_GAP = 80L    // ms
    private const val PATTERN_GAP = 200L // ms between repetitions
    
    /**
     * Generates the heartbeat pattern as a list of durations (ON, OFF, ON, OFF...).
     *
     * Pattern: • • • (3 short pulses)
     *
     * @return List of durations in milliseconds, alternating between ON and OFF periods
     */
    fun generateHeartbeat(): List<Long> {
        return listOf(
            SHORT_PULSE,  // ON
            SHORT_GAP,    // OFF
            SHORT_PULSE,  // ON
            SHORT_GAP,    // OFF
            SHORT_PULSE,  // ON
            PATTERN_GAP   // OFF (final pause)
        )
    }
    
    /**
     * Generates a heartbeat with unique ID (slightly varying the intervals).
     *
     * This allows differentiating devices in the future by modulating intervals
     * based on device ID. Currently uses the standard pattern.
     *
     * @param deviceId Unique identifier for the device
     * @return List of durations in milliseconds for the device-specific heartbeat
     */
    fun generateHeartbeatWithId(deviceId: String): List<Long> {
        // Currently uses the standard pattern
        // In the future could modulate intervals based on ID
        return generateHeartbeat()
    }
    
    /**
     * Returns the total duration of the heartbeat pattern in milliseconds.
     *
     * @return Total pattern duration including all pulses and gaps
     */
    fun getPatternDuration(): Long {
        return generateHeartbeat().sum()
    }
    
    /**
     * Recommended interval between heartbeat transmissions in milliseconds.
     *
     * Set to 3 seconds to avoid signal saturation while maintaining presence detection.
     */
    const val HEARTBEAT_INTERVAL = 3000L
    
    /**
     * Emergency pattern (more aggressive): SOS in Morse code.
     *
     * Pattern: • • •  — — —  • • •
     *
     * @return List of durations for the SOS emergency signal
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
