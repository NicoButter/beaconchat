package com.nicobutter.beaconchat.emitter

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.nicobutter.beaconchat.domain.SignalConfig
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

/**
 * Emits an emergency beacon via Bluetooth Low Energy advertising.
 *
 * Broadcasts a non-connectable BLE advertisement containing:
 * - Service UUID [EMERGENCY_SERVICE_UUID] so scanners can filter by service
 * - 1-byte service data payload encoding the [com.nicobutter.beaconchat.domain.EmergencyType] ordinal
 *
 * This channel is silent and invisible — ideal for [com.nicobutter.beaconchat.domain.EmergencyMode.DISCREET].
 * Compatible with [com.nicobutter.beaconchat.scanner.BleScanner] on the receiving side.
 *
 * Advertising continues indefinitely (timeout = 0) until [stop] is called.
 */
class BleEmitter(private val context: Context) : SignalEmitter {

    companion object {
        /** Service UUID that identifies a BeaconChat emergency advertisement. BECE = BeaconChat Emergency */
        val EMERGENCY_SERVICE_UUID: UUID = UUID.fromString("0000BECE-0000-1000-8000-00805f9b34fb")
        private const val TAG = "BleEmitter"
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val advertiser get() = bluetoothManager.adapter?.bluetoothLeAdvertiser

    private var isAdvertising = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            Log.d(TAG, "BLE emergency advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            Log.e(TAG, "BLE advertising failed with code $errorCode")
        }
    }

    /**
     * Starts BLE advertising. Does not need a coroutine — advertising runs in the BLE stack.
     * The [scope] parameter is accepted to satisfy the [SignalEmitter] interface but is unused here.
     */
    override fun start(config: SignalConfig, scope: CoroutineScope) {
        if (isAdvertising) return

        val adv = advertiser ?: run {
            Log.e(TAG, "BluetoothLeAdvertiser not available (BT off or not supported)")
            return
        }

        val typeCode = config.emergencyType.ordinal.toByte()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(EMERGENCY_SERVICE_UUID))
            .addServiceData(ParcelUuid(EMERGENCY_SERVICE_UUID), byteArrayOf(typeCode))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()

        try {
            adv.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_ADVERTISE permission missing", e)
        }
    }

    override fun stop() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Log.d(TAG, "BLE emergency advertising stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_ADVERTISE permission missing on stop", e)
        }
    }
}
