package com.nicobutter.beaconchat.transceiver

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class FlashlightController(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun transmit(timings: List<Long>) {
        val id = cameraId ?: return
        
        withContext(Dispatchers.IO) {
            try {
                for (i in timings.indices) {
                    val duration = timings[i]
                    val isTurnOn = i % 2 == 0 // Even indices are ON, Odd are OFF

                    if (isTurnOn) {
                        setTorchMode(id, true)
                    } else {
                        setTorchMode(id, false)
                    }
                    delay(duration)
                }
                // Ensure off at the end
                setTorchMode(id, false)
            } catch (e: Exception) {
                e.printStackTrace()
                setTorchMode(id, false)
            }
        }
    }

    private fun setTorchMode(id: String, enabled: Boolean) {
        try {
            cameraManager.setTorchMode(id, enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
