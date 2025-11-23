package com.nicobutter.beaconchat.mesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BLEMeshController(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    // Service UUID for BeaconChat (BEEF = BeaconChat)
    private val SERVICE_UUID = UUID.fromString("0000BEEF-0000-1000-8000-00805f9b34fb")

    // State flows
    private val _peers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val peers: StateFlow<List<MeshPeer>> = _peers.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    companion object {
        private const val TAG = "BLEMeshController"
    }

    init {
        Log.d(TAG, "BLEMeshController initialized")
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    /** Start advertising this device's presence */
    fun startAdvertising(callsign: String) {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        try {
            val settings =
                    AdvertiseSettings.Builder()
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            .setConnectable(true)
                            .setTimeout(0)
                            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                            .build()

            val data =
                    AdvertiseData.Builder()
                            .setIncludeDeviceName(true)
                            .addServiceUuid(ParcelUuid(SERVICE_UUID))
                            .addServiceData(ParcelUuid(SERVICE_UUID), callsign.toByteArray())
                            .build()

            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            _isAdvertising.value = true
            Log.d(TAG, "Started advertising with callsign: $callsign")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting advertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
        }
    }

    /** Stop advertising */
    fun stopAdvertising() {
        if (!checkBluetoothPermissions()) return

        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            _isAdvertising.value = false
            Log.d(TAG, "Stopped advertising")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping advertising", e)
        }
    }

    /** Start scanning for other devices */
    fun startScanning() {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        try {
            val scanSettings =
                    ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                            .build()

            val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()

            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Started scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting scan", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
        }
    }

    /** Stop scanning */
    fun stopScanning() {
        if (!checkBluetoothPermissions()) return

        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            _isScanning.value = false
            Log.d(TAG, "Stopped scanning")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping scan", e)
        }
    }

    /** Cleanup resources */
    fun cleanup() {
        stopAdvertising()
        stopScanning()
        _peers.value = emptyList()
        Log.d(TAG, "Cleaned up")
    }

    private val advertiseCallback =
            object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.d(TAG, "Advertising started successfully")
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG, "Advertising failed with error: $errorCode")
                    _isAdvertising.value = false
                }
            }

    private val scanCallback =
            object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.let { processScanResult(it) }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    results?.forEach { processScanResult(it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "Scan failed with error: $errorCode")
                    _isScanning.value = false
                }
            }

    private fun processScanResult(result: ScanResult) {
        try {
            val device = result.device
            val rssi = result.rssi
            val deviceName =
                    if (checkBluetoothPermissions()) {
                        result.scanRecord?.deviceName ?: device.name ?: "Unknown"
                    } else {
                        "Unknown"
                    }

            // Extract callsign from service data
            val serviceData = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
            val callsign = serviceData?.let { String(it) } ?: "UNKNOWN"

            val peer =
                    MeshPeer(
                            id = device.address,
                            name = deviceName,
                            callsign = callsign,
                            rssi = rssi,
                            lastSeen = System.currentTimeMillis()
                    )

            // Update peers list
            val currentPeers = _peers.value.toMutableList()
            val existingIndex = currentPeers.indexOfFirst { it.id == peer.id }

            if (existingIndex >= 0) {
                currentPeers[existingIndex] = peer
            } else {
                currentPeers.add(peer)
                Log.d(TAG, "Discovered new peer: ${peer.callsign} (${peer.name})")
            }

            _peers.value = currentPeers
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception processing scan result", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scan result", e)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissions =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    listOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    listOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }

        return permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun updateBluetoothState() {
        _bluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
    }
}
