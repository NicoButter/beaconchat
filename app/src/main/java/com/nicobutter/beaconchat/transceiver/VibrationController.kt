package com.nicobutter.beaconchat.transceiver

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Controls vibration transmission for BeaconChat signaling.
 *
 * This controller manages device vibration motor to transmit timing-based
 * signals. It converts timing sequences into vibration on/off patterns
 * suitable for haptic communication, with proper API compatibility
 * across Android versions.
 */
class VibrationController(context: Context) {
    private val vibrator: Vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as
                                VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

    private val transmissionMutex = Mutex()

    /** Callback for visual debugging of transmission state. */
    var onStateChange: ((Boolean, Int, Int) -> Unit)? = null

    init {
        Log.d(TAG, "VibrationController initialized. Has vibrator: ${vibrator.hasVibrator()}")
    }

    /**
     * Transmits a sequence of timing durations using device vibration.
     *
     * Converts the timing list into vibration on/off states where even indices
     * represent vibration periods and odd indices represent pause periods.
     * Checks for vibrator availability before transmission.
     *
     * @param timings List of durations in milliseconds, alternating vibration/pause periods
     */
    suspend fun transmit(timings: List<Long>) {
        if (!vibrator.hasVibrator()) {
            Log.e(TAG, "Device does not have a vibrator")
            return
        }

        // Prevent concurrent transmissions
        transmissionMutex.withLock {
            Log.d(TAG, "Starting vibration transmission with ${timings.size} timings")

            withContext(Dispatchers.IO) {
                try {
                    // Ensure vibrator is off before starting
                    vibrator.cancel()

                    for (i in timings.indices) {
                        val duration = timings[i]
                        val isVibrate = i % 2 == 0 // Even indices are ON, Odd are OFF

                        if (isVibrate) {
                            Log.d(TAG, "[$i/${timings.size}] VIBRATE for ${duration}ms")
                            vibrateFor(duration)
                            onStateChange?.invoke(true, i, timings.size)
                        } else {
                            Log.d(TAG, "[$i/${timings.size}] PAUSE for ${duration}ms")
                            onStateChange?.invoke(false, i, timings.size)
                            delay(duration)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during vibration transmission", e)
                    e.printStackTrace()
                    onStateChange?.invoke(false, -1, timings.size)
                } finally {
                    // ALWAYS ensure vibrator is off when done
                    Log.d(TAG, "Vibration transmission complete, stopping vibrator")
                    vibrator.cancel()
                    onStateChange?.invoke(false, timings.size, timings.size)
                }
            }
        }
    }

    /**
     * Vibrates the device for the specified duration with API compatibility.
     *
     * Uses VibrationEffect for Android O+ devices and legacy vibrate() method
     * for older versions. Waits for the vibration to complete before returning.
     *
     * @param durationMs Duration to vibrate in milliseconds
     */
    private suspend fun vibrateFor(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(durationMs)
            }
            // Wait for the vibration to complete
            delay(durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating for ${durationMs}ms", e)
            e.printStackTrace()
        }
    }

    /**
     * Cleans up vibration resources and stops any ongoing vibration.
     *
     * This method should be called when the controller is no longer needed
     * to ensure proper resource cleanup and prevent vibration from continuing.
     */
    fun cleanup() {
        try {
            vibrator.cancel()
            // Give hardware time to release
            Thread.sleep(50)
            Log.d(TAG, "Vibrator cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
        // Reset callback
        onStateChange = null
    }

    companion object {
        private const val TAG = "VibrationController"
    }
}
