package com.nicobutter.beaconchat.scanner

/**
 * Contract for all signal reception channels.
 *
 * Implementations: [BleScanner], plus the existing camera-based detectors
 * ([com.nicobutter.beaconchat.transceiver.LightDetector],
 * [com.nicobutter.beaconchat.lightmap.LightScanner]) which can be adapted
 * to this interface when a unified scanner is needed.
 */
interface SignalScanner {
    fun start()
    fun stop()
}
