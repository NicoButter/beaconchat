package com.nicobutter.beaconchat.mesh

import android.Manifest
import android.bluetooth.*
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

/**
 * Manages Bluetooth LE mesh networking for BeaconChat device communication.
 *
 * This controller handles all Bluetooth LE operations for the BeaconChat mesh network,
 * including device discovery, advertising, GATT server/client operations, and message
 * exchange. It provides a reactive interface using Kotlin Flows for UI integration.
 *
 * Key features:
 * - BLE advertising with custom service UUIDs and callsign data
 * - Device scanning with service filtering
 * - GATT server for receiving messages
 * - GATT client for sending messages to peers
 * - Reactive state management with error handling
 *
 * @property context Android context for Bluetooth operations
 * @property peers Flow of discovered peer devices
 * @property messages Flow of chat messages (sent and received)
 * @property isAdvertising Flow indicating if device is currently advertising
 * @property isScanning Flow indicating if device is currently scanning
 * @property bluetoothEnabled Flow indicating Bluetooth adapter state
 * @property isSending Flow indicating if a message is currently being sent
 * @property lastError Flow containing the last error message (null if no error)
 */
class BLEMeshController(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGattServer: BluetoothGattServer? = null

    // Service UUID for BeaconChat (BEEF = BeaconChat)
    private val SERVICE_UUID = UUID.fromString("0000BEEF-0000-1000-8000-00805f9b34fb")

    // Chat Service & Characteristic UUIDs (using valid hex values)
    private val CHAT_SERVICE_UUID = UUID.fromString("0000C4A7-0000-1000-8000-00805f9b34fb")
    private val MESSAGE_CHARACTERISTIC_UUID =
            UUID.fromString("00004D51-0000-1000-8000-00805f9b34fb")

    // State flows
    private val _peers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val peers: StateFlow<List<MeshPeer>> = _peers.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Clears the last error message.
     */
    fun clearError() {
        _lastError.value = null
    }

    companion object {
        private const val TAG = "BLEMeshController"
    }

    init {
        Log.d(TAG, "BLEMeshController initialized")
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    /** Start advertising this device's presence and GATT Server */
    fun startAdvertising(callsign: String) {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        // Si ya estamos anunciando, no hacer nada
        if (_isAdvertising.value) return

        try {
            // Re-obtener el advertiser si es nulo (sucede si BT se activó después del init)
            if (bluetoothLeAdvertiser == null) {
                bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
            }

            // Start GATT Server
            startGattServer()

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
            _lastError.value = "Error de seguridad en BLE"
        } catch (e: Exception) {
            Log.e(TAG, "Error starting advertising", e)
            _lastError.value = "No se pudo iniciar visibilidad"
            _isAdvertising.value = false
        }
    }

    /** Stop advertising */
    fun stopAdvertising() {
        if (!checkBluetoothPermissions()) return

        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            stopGattServer()
            _isAdvertising.value = false
            Log.d(TAG, "Stopped advertising")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping advertising", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising", e)
            _isAdvertising.value = false // Forzar estado aunque falle el hardware
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

        // Si ya estamos escaneando, no hacer nada
        if (_isScanning.value) return

        try {
            // Re-obtener el scanner si es nulo
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            }

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
            _lastError.value = "Error de seguridad en Escaneo"
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            _lastError.value = "No se pudo iniciar escaneo"
            _isScanning.value = false
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
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
            _isScanning.value = false
        }
    }

    /**
     * Send a message to a peer device.
     *
     * Establishes a GATT connection to the specified peer and writes the message
     * to the chat characteristic. The message format is "CALLSIGN:MESSAGE".
     *
     * @param peerAddress Bluetooth MAC address of the target device
     * @param message Text content to send
     * @param myCallsign Callsign of the sender (current user)
     */
    fun sendMessage(peerAddress: String, message: String, myCallsign: String) {
        if (!checkBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions for sendMessage")
            _lastError.value = "Faltan permisos de Bluetooth"
            return
        }
        
        // No permitir enviar si ya hay un envío en curso
        if (_isSending.value) {
            Log.w(TAG, "Already sending a message, please wait")
            return
        }
        
        _isSending.value = true
        _lastError.value = null

        try {
            val device = bluetoothAdapter?.getRemoteDevice(peerAddress)
            if (device == null) {
                Log.e(TAG, "Could not get remote device for address: $peerAddress")
                _lastError.value = "No se pudo conectar al dispositivo"
                _isSending.value = false
                return
            }
            
            Log.d(TAG, "Connecting to $peerAddress to send message: $message")

            // Connect to GATT Server on peer
            device.connectGatt(
                    context,
                    false,
                    object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                                gatt: BluetoothGatt?,
                                status: Int,
                                newState: Int
                        ) {
                            Log.d(TAG, "Connection state changed: status=$status, newState=$newState")
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.d(TAG, "Connected to GATT server. Discovering services...")
                                gatt?.discoverServices()
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d(TAG, "Disconnected from GATT server")
                                _isSending.value = false
                                gatt?.close()
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            Log.d(TAG, "Services discovered with status: $status")
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                val services = gatt?.services
                                Log.d(TAG, "Available services: ${services?.map { it.uuid }}")
                                
                                val service = gatt?.getService(CHAT_SERVICE_UUID)
                                if (service == null) {
                                    Log.e(TAG, "Chat service not found! Available: ${services?.map { it.uuid }}")
                                    _lastError.value = "Servicio de chat no encontrado en el peer"
                                    _isSending.value = false
                                    gatt?.disconnect()
                                    return
                                }
                                
                                val characteristic = service.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID)

                                if (characteristic != null) {
                                    // Format: "CALLSIGN:MESSAGE"
                                    val payload = "$myCallsign:$message"
                                    characteristic.value = payload.toByteArray(Charsets.UTF_8)
                                    characteristic.writeType =
                                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                                    val success = gatt.writeCharacteristic(characteristic)
                                    Log.d(TAG, "Write characteristic initiated: $success")

                                    if (success) {
                                        // Mensaje iniciado con éxito (se confirma en onCharacteristicWrite)
                                    } else {
                                        _lastError.value = "Error al iniciar escritura"
                                        _isSending.value = false
                                        gatt.disconnect()
                                    }
                                } else {
                                    Log.e(TAG, "Message characteristic not found in service")
                                    _lastError.value = "Característica de mensaje no encontrada"
                                    _isSending.value = false
                                    gatt.disconnect()
                                }
                            } else {
                                Log.w(TAG, "onServicesDiscovered received: $status")
                                _lastError.value = "Error descubriendo servicios: $status"
                                _isSending.value = false
                                gatt?.disconnect()
                            }
                        }

                        override fun onCharacteristicWrite(
                                gatt: BluetoothGatt?,
                                characteristic: BluetoothGattCharacteristic?,
                                status: Int
                        ) {
                            _isSending.value = false
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                Log.d(TAG, "Message sent successfully!")
                                
                                // AGREGAR EL MENSAJE LOCALMENTE SOLO AL TENER ÉXITO
                                val chatMessage =
                                        ChatMessage(
                                                senderId = "ME",
                                                senderName = myCallsign,
                                                recipientId = peerAddress,
                                                content = message,
                                                timestamp = System.currentTimeMillis(),
                                                isFromMe = true
                                        )
                                val currentMessages = _messages.value.toMutableList()
                                currentMessages.add(chatMessage)
                                _messages.value = currentMessages
                                
                                _lastError.value = null
                            } else {
                                Log.e(TAG, "Failed to send message. Status: $status")
                                _lastError.value = "Error al escribir mensaje: $status"
                            }
                            gatt?.disconnect() // Disconnect after sending
                        }
                    },
                    BluetoothDevice.TRANSPORT_LE // Forzar transporte LE
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception sending message", e)
            _isSending.value = false
            _lastError.value = "Error de permisos"
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            _isSending.value = false
            _lastError.value = "Error desconocido al enviar"
        }
    }

    /**
     * Starts the GATT server for receiving messages from other devices.
     *
     * Creates a GATT service with a writable characteristic for chat messages.
     * This server runs while the device is advertising.
     */
    private fun startGattServer() {
        if (!checkBluetoothPermissions()) return

        try {
            val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)

            val service =
                    BluetoothGattService(
                            CHAT_SERVICE_UUID,
                            BluetoothGattService.SERVICE_TYPE_PRIMARY
                    )

            val characteristic =
                    BluetoothGattCharacteristic(
                            MESSAGE_CHARACTERISTIC_UUID,
                            BluetoothGattCharacteristic.PROPERTY_WRITE,
                            BluetoothGattCharacteristic.PERMISSION_WRITE
                    )

            service.addCharacteristic(characteristic)
            bluetoothGattServer?.addService(service)
            Log.d(TAG, "GATT Server started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting GATT server", e)
        }
    }

    /**
     * Stops the GATT server and cleans up resources.
     */
    private fun stopGattServer() {
        if (!checkBluetoothPermissions()) return
        try {
            bluetoothGattServer?.close()
            bluetoothGattServer = null
            Log.d(TAG, "GATT Server stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping GATT server", e)
        }
    }

    private val gattServerCallback =
            object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(
                        device: BluetoothDevice?,
                        status: Int,
                        newState: Int
                ) {
                    super.onConnectionStateChange(device, status, newState)
                    Log.d(TAG, "GATT Server connection state change: $newState")
                }

                override fun onCharacteristicWriteRequest(
                        device: BluetoothDevice?,
                        requestId: Int,
                        characteristic: BluetoothGattCharacteristic?,
                        preparedWrite: Boolean,
                        responseNeeded: Boolean,
                        offset: Int,
                        value: ByteArray?
                ) {
                    super.onCharacteristicWriteRequest(
                            device,
                            requestId,
                            characteristic,
                            preparedWrite,
                            responseNeeded,
                            offset,
                            value
                    )

                    if (MESSAGE_CHARACTERISTIC_UUID == characteristic?.uuid) {
                        value?.let {
                            val messageString = String(it, Charsets.UTF_8)
                            Log.d(TAG, "Received message: $messageString from ${device?.address}")

                            // Parse "CALLSIGN:MESSAGE"
                            val parts = messageString.split(":", limit = 2)
                            if (parts.size == 2) {
                                val senderCallsign = parts[0]
                                val content = parts[1]

                                val chatMessage =
                                        ChatMessage(
                                                senderId = device?.address ?: "UNKNOWN",
                                                senderName = senderCallsign,
                                                recipientId = "ME", // Este dispositivo es el receptor
                                                content = content,
                                                timestamp = System.currentTimeMillis(),
                                                isFromMe = false
                                        )

                                val currentMessages = _messages.value.toMutableList()
                                currentMessages.add(chatMessage)
                                _messages.value = currentMessages
                                
                                Log.d(TAG, "Message added to list. Total messages: ${currentMessages.size}")
                            }
                        }

                        if (responseNeeded) {
                            bluetoothGattServer?.sendResponse(
                                    device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    null
                            )
                        }
                    }
                }
            }

    /** Cleanup resources */
    fun cleanup() {
        stopAdvertising()
        stopScanning()
        _peers.value = emptyList()
        _messages.value = emptyList()
        _isSending.value = false
        _lastError.value = null
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

    /**
     * Processes a BLE scan result and updates the peers list.
     *
     * Extracts device information, callsign from service data, and RSSI.
     * Updates existing peers or adds new ones to the peers flow.
     *
     * @param result The scan result containing device information
     */
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

    /**
     * Checks if all required Bluetooth permissions are granted.
     *
     * Handles different permission requirements for Android versions:
     * - Android 12+ (API 31+): BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT
     * - Older versions: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION
     *
     * @return true if all required permissions are granted, false otherwise
     */
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

    /**
     * Updates the Bluetooth enabled state flow based on current adapter state.
     *
     * Should be called when the Bluetooth adapter state changes to keep
     * the UI synchronized with the actual Bluetooth state.
     */
    fun updateBluetoothState() {
        val isEnabled = bluetoothAdapter?.isEnabled == true
        _bluetoothEnabled.value = isEnabled
        
        // Si se desactivó el bluetooth, resetear estados
        if (!isEnabled) {
            _isAdvertising.value = false
            _isScanning.value = false
            _peers.value = emptyList()
        }
    }
}
