package com.nicobutter.beaconchat.controller

import android.util.Log
import com.nicobutter.beaconchat.domain.EmergencyMode
import com.nicobutter.beaconchat.domain.EmergencyState
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.domain.SignalConfig
import com.nicobutter.beaconchat.emitter.BleEmitter
import com.nicobutter.beaconchat.emitter.LightEmitter
import com.nicobutter.beaconchat.emitter.SignalEmitter
import com.nicobutter.beaconchat.emitter.SoundEmitter
import com.nicobutter.beaconchat.emitter.VibrationEmitter
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrates all signal emitters for emergency transmission.
 *
 * The UI only needs to call [startEmergency] / [stopEmergency]:
 * ```
 * emergencyManager.startEmergency(EmergencyType.SOS, EmergencyMode.ALL)
 * ```
 *
 * The manager selects the correct emitters for the chosen [EmergencyMode],
 * encodes the message, and starts them all. [state] exposes the current
 * transmission state as a [StateFlow] for reactive UI observation.
 *
 * @param scope Coroutine scope tied to the Activity lifecycle (e.g. `lifecycleScope`).
 *              Cancelling the scope automatically stops all running emitters.
 */
class EmergencyManager(
    private val lightEmitter: LightEmitter,
    private val vibrationEmitter: VibrationEmitter,
    private val soundEmitter: SoundEmitter,
    private val bleEmitter: BleEmitter,
    private val morseEncoder: MorseEncoder,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "EmergencyManager"
    }

    private val _state = MutableStateFlow(EmergencyState())
    val state: StateFlow<EmergencyState> = _state.asStateFlow()

    /**
     * Starts the emergency signal.
     *
     * Encodes [type.morseMessage] using [MorseEncoder], builds a [SignalConfig],
     * and delegates to all emitters that match [mode].
     * Calling this while already active stops the previous emission first.
     */
    fun startEmergency(type: EmergencyType, mode: EmergencyMode) {
        if (_state.value.isActive) stopEmergency()

        Log.d(TAG, "Starting emergency: type=$type, mode=$mode")

        val timings = morseEncoder.encode(type.morseMessage)
        val config = SignalConfig(timings = timings, emergencyType = type)

        emittersFor(mode).forEach { it.start(config, scope) }

        _state.value = EmergencyState(isActive = true, type = type, mode = mode)
    }

    /**
     * Stops all active emitters and resets state.
     */
    fun stopEmergency() {
        Log.d(TAG, "Stopping emergency")
        allEmitters().forEach { it.stop() }
        _state.value = EmergencyState(isActive = false)
    }

    private fun emittersFor(mode: EmergencyMode): List<SignalEmitter> = when (mode) {
        EmergencyMode.ALL        -> allEmitters()
        EmergencyMode.LIGHT      -> listOf(lightEmitter)
        EmergencyMode.VIBRATION  -> listOf(vibrationEmitter)
        EmergencyMode.SOUND      -> listOf(soundEmitter)
        EmergencyMode.BLE        -> listOf(bleEmitter)
        EmergencyMode.DISCREET   -> listOf(bleEmitter)
    }

    private fun allEmitters(): List<SignalEmitter> =
        listOf(lightEmitter, vibrationEmitter, soundEmitter, bleEmitter)
}
