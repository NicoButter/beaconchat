package com.nicobutter.beaconchat.emitter

import com.nicobutter.beaconchat.domain.SignalConfig
import com.nicobutter.beaconchat.transceiver.VibrationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Emits signals using the device haptic motor.
 *
 * Wraps [VibrationController] and loops continuously until [stop] is called.
 */
class VibrationEmitter(private val controller: VibrationController) : SignalEmitter {

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
