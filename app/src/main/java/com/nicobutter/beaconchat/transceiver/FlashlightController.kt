package com.nicobutter.beaconchat.transceiver

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Controls flashlight transmission for BeaconChat signaling.
 *
 * This controller manages the device camera flashlight (torch) to transmit
 * timing-based signals. It converts timing sequences into flashlight on/off
 * patterns suitable for optical communication.
 */
class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private val transmissionMutex = Mutex()
    private var isFlashlightOn = false
    @Volatile private var isStopped = false

    /** Callback for visual debugging of transmission state. */
    var onStateChange: ((Boolean, Int, Int) -> Unit)? = null

    init {
        try {
            cameraId =
                    cameraManager.cameraIdList.firstOrNull { id ->
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        characteristics.get(
                                android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                        ) == true
                    }
            Log.d(TAG, "FlashlightController initialized. Camera ID: $cameraId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FlashlightController", e)
            e.printStackTrace()
        }
    }

    /**
     * Transmits a sequence of timing durations using the flashlight.
     *
     * Converts the timing list into flashlight on/off states where even indices
     * represent ON periods and odd indices represent OFF periods.
     *
     * @param timings List of durations in milliseconds, alternating ON/OFF periods
     */
    suspend fun transmit(timings: List<Long>) {
        val id =
                cameraId
                        ?: run {
                            Log.e(TAG, "No camera ID available for flashlight")
                            return
                        }

        // Prevent concurrent transmissions
        transmissionMutex.withLock {
            Log.d(TAG, "Starting transmission with ${timings.size} timings")

            // Reset stop flag
            isStopped = false

            withContext(Dispatchers.IO) {
                try {
                    // Ensure flashlight starts OFF
                    ensureFlashlightOff(id)
                    
                    Log.w(TAG, "=== TRANSMISSION START: ${timings.size} timings ===")
                    for (i in timings.indices) {
                        if (isStopped) {
                            Log.d(TAG, "Transmission stopped at step $i")
                            break
                        }

                        val duration = timings[i]
                        val isTurnOn = i % 2 == 0
                        val startTime = System.currentTimeMillis()
                        
                        if (isTurnOn) {
                            Log.w(TAG, "[$i] ON for ${duration}ms (expected end: +${duration}ms)")
                            setTorchMode(id, true)
                            onStateChange?.invoke(true, i, timings.size)
                        } else {
                            Log.w(TAG, "[$i] OFF for ${duration}ms (expected end: +${duration}ms)")
                            setTorchMode(id, false)
                            onStateChange?.invoke(false, i, timings.size)
                        }
                        
                        delay(duration)
                        val actualDuration = System.currentTimeMillis() - startTime
                        val drift = actualDuration - duration
                        if (kotlin.math.abs(drift) > 50) {
                            Log.w(TAG, "⚠ TIMING DRIFT at [$i]: expected ${duration}ms, actual ${actualDuration}ms (drift: ${drift}ms)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during transmission", e)
                    e.printStackTrace()
                    onStateChange?.invoke(false, -1, timings.size)
                } finally {
                    // ALWAYS ensure flashlight is off when done
                    Log.d(TAG, "Transmission complete, ensuring flashlight is off")
                    ensureFlashlightOff(id)
                    onStateChange?.invoke(false, timings.size, timings.size)
                    isStopped = false // Reset flag
                }
            }
        }
    }

    private fun setTorchMode(id: String, enabled: Boolean) {
        try {
            cameraManager.setTorchMode(id, enabled)
            isFlashlightOn = enabled
            Log.d(TAG, "Torch mode set to $enabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch mode to $enabled", e)
            e.printStackTrace()
            // If we failed to turn it on/off, try to recover
            if (enabled) {
                // Failed to turn on, make sure we're in a clean state
                try {
                    cameraManager.setTorchMode(id, false)
                    isFlashlightOn = false
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to recover from torch error", e2)
                }
            }
        }
    }

    /**
     * Ensures the flashlight is turned off and waits for hardware to respond.
     *
     * This method provides a reliable way to turn off the flashlight with
     * error handling and a small delay to ensure the hardware state is stable.
     *
     * @param id The camera ID to control
     */
    private fun ensureFlashlightOff(id: String) {
        try {
            if (isFlashlightOn) {
                Log.d(TAG, "Flashlight was on, turning it off")
            }
            cameraManager.setTorchMode(id, false)
            isFlashlightOn = false
            // Small delay to ensure the hardware has time to respond
            Thread.sleep(50)
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring flashlight is off", e)
            e.printStackTrace()
        }
    }

    /**
     * Stops any ongoing transmission and immediately turns off the flashlight.
     *
     * This method sets the stop flag to interrupt the transmission loop and
     * immediately turns off the flashlight for safety.
     */
    fun stop() {
        isStopped = true
        // Immediately turn off flashlight
        cameraId?.let { id ->
            try {
                ensureFlashlightOff(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping flashlight", e)
            }
        }
    }

    /**
     * Cleans up resources and ensures the flashlight is turned off.
     *
     * This method should be called when the controller is no longer needed
     * to ensure proper resource cleanup and prevent the flashlight from
     * remaining on.
     */
    fun cleanup() {
        // Set stop flag to interrupt any ongoing transmission
        isStopped = true
        cameraId?.let { id ->
            try {
                ensureFlashlightOff(id)
                Log.d(TAG, "Flashlight cleaned up successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    companion object {
        private const val TAG = "FlashlightController"
    }
}
