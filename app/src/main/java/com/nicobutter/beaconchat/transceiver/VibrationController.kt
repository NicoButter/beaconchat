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

    // Callback para debugging visual
    var onStateChange: ((Boolean, Int, Int) -> Unit)? = null

    init {
        Log.d(TAG, "VibrationController initialized. Has vibrator: ${vibrator.hasVibrator()}")
    }

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

    fun cleanup() {
        try {
            vibrator.cancel()
            Log.d(TAG, "Vibrator cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    companion object {
        private const val TAG = "VibrationController"
    }
}
