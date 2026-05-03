package com.nicobutter.beaconchat.emitter

import com.nicobutter.beaconchat.domain.SignalConfig
import com.nicobutter.beaconchat.transceiver.FlashlightController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Emits signals using the camera flashlight (LED torch).
 *
 * Wraps [FlashlightController] and loops continuously until [stop] is called.
 */
class LightEmitter(private val controller: FlashlightController) : SignalEmitter {

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
        controller.stop()
    }
}
