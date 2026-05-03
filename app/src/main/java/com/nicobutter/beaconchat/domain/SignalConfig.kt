package com.nicobutter.beaconchat.domain

/**
 * Carries the encoded signal ready for transmission.
 *
 * Built by [com.nicobutter.beaconchat.controller.EmergencyManager] from an [EmergencyType]
 * and passed to each [com.nicobutter.beaconchat.emitter.SignalEmitter].
 *
 * @property timings Alternating ON/OFF durations in milliseconds (output of MorseEncoder).
 * @property emergencyType The semantic intent — used by BleEmitter to tag the BLE advertisement.
 */
data class SignalConfig(
    val timings: List<Long>,
    val emergencyType: EmergencyType
)
