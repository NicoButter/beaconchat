package com.nicobutter.beaconchat.transceiver

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private val transmissionMutex = Mutex()
    private var isFlashlightOn = false

    // Callback para debugging visual
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

            withContext(Dispatchers.IO) {
                try {
                    // Ensure flashlight is off before starting
                    ensureFlashlightOff(id)

                    for (i in timings.indices) {
                        val duration = timings[i]
                        val isTurnOn = i % 2 == 0 // Even indices are ON, Odd are OFF

                        if (isTurnOn) {
                            Log.d(TAG, "[$i/${timings.size}] ON for ${duration}ms")
                            setTorchMode(id, true)
                            onStateChange?.invoke(true, i, timings.size)
                        } else {
                            Log.d(TAG, "[$i/${timings.size}] OFF for ${duration}ms")
                            setTorchMode(id, false)
                            onStateChange?.invoke(false, i, timings.size)
                        }
                        delay(duration)
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

    fun cleanup() {
        cameraId?.let { id ->
            try {
                ensureFlashlightOff(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }

    companion object {
        private const val TAG = "FlashlightController"
    }
}
