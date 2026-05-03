package com.nicobutter.beaconchat.emitter

import com.nicobutter.beaconchat.domain.SignalConfig
import com.nicobutter.beaconchat.transceiver.SoundController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Emits signals using 18.5 kHz ultrasound (nearly inaudible).
 *
 * Wraps [SoundController] and loops continuously until [stop] is called.
 */
class SoundEmitter(private val controller: SoundController) : SignalEmitter {

    private var job: Job? = null

    override fun start(config: SignalConfig, scope: CoroutineScope) {
        stop()
        job = scope.launch {
            while (isActive) {
                controller.transmit(config.timings)
                delay(500L)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        controller.cleanup()
    }
}
