package com.nicobutter.beaconchat.scanner

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.nicobutter.beaconchat.domain.EmergencyType
import com.nicobutter.beaconchat.emitter.BleEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A detected emergency beacon from another BeaconChat device.
 *
 * @property deviceAddress Bluetooth MAC address of the transmitting device.
 * @property emergencyType The emergency type decoded from the BLE service data.
 * @property rssi Signal strength in dBm — lower (more negative) means farther away.
 * @property lastSeen Epoch milliseconds of the last received advertisement.
 */
data class DetectedEmergency(
    val deviceAddress: String,
    val emergencyType: EmergencyType,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
) {
    fun signalQuality(): String = when {
        rssi >= -50 -> "Excelente"
        rssi >= -70 -> "Buena"
        rssi >= -85 -> "Regular"
        else -> "Débil"
    }
}

/**
 * Scans for emergency BLE beacons emitted by [BleEmitter].
 *
 * Filters advertisements by [BleEmitter.EMERGENCY_SERVICE_UUID] and decodes
 * the 1-byte service data payload into an [EmergencyType].
 * Results are exposed as a [StateFlow] for reactive UI updates.
 *
 * Requires BLUETOOTH_SCAN permission (already in AndroidManifest).
 */
class BleScanner(private val context: Context) : SignalScanner {

    companion object {
        private const val TAG = "BleScanner"
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val leScanner get() = bluetoothManager.adapter?.bluetoothLeScanner

    private val _detectedEmergencies = MutableStateFlow<List<DetectedEmergency>>(emptyList())
    val detectedEmergencies: StateFlow<List<DetectedEmergency>> = _detectedEmergencies.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            Log.e(TAG, "BLE scan failed with code $errorCode")
        }
    }

    private fun processResult(result: ScanResult) {
        val serviceData = result.scanRecord
            ?.getServiceData(ParcelUuid(BleEmitter.EMERGENCY_SERVICE_UUID))
            ?: return

        val typeOrdinal = serviceData.firstOrNull()?.toInt() ?: return
        val type = EmergencyType.values().getOrNull(typeOrdinal) ?: return

        val incoming = DetectedEmergency(
            deviceAddress = result.device.address,
            emergencyType = type,
            rssi = result.rssi
        )

        val updated = _detectedEmergencies.value.toMutableList()
        val idx = updated.indexOfFirst { it.deviceAddress == incoming.deviceAddress }
        if (idx >= 0) updated[idx] = incoming else updated.add(incoming)
        _detectedEmergencies.value = updated
    }

    override fun start() {
        val scanner = leScanner ?: run {
            Log.e(TAG, "BLE scanner not available (BT off or not supported)")
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleEmitter.EMERGENCY_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "BLE emergency scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing", e)
        }
    }

    override fun stop() {
        try {
            leScanner?.stopScan(scanCallback)
            _isScanning.value = false
            Log.d(TAG, "BLE emergency scan stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_SCAN permission missing on stop", e)
        }
    }

    /** Clears the list of detected emergencies. */
    fun clearDetected() {
        _detectedEmergencies.value = emptyList()
    }
}
