package com.nicobutter.beaconchat.emitter

import com.nicobutter.beaconchat.domain.SignalConfig
import kotlinx.coroutines.CoroutineScope

/**
 * Contract for all signal transmission channels.
 *
 * Implementations: [LightEmitter], [VibrationEmitter], [SoundEmitter], [BleEmitter].
 *
 * [start] launches the emitter and keeps it running until [stop] is called.
 * The provided [scope] is used to run the transmission coroutine; cancelling the
 * scope automatically stops the emitter.
 */
interface SignalEmitter {
    fun start(config: SignalConfig, scope: CoroutineScope)
    fun stop()
}
