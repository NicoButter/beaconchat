package com.nicobutter.beaconchat.mesh

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.nicobutter.beaconchat.mesh.ble.AndroidMeshTransport
import com.nicobutter.beaconchat.mesh.ble.MeshDebugLog
import com.nicobutter.beaconchat.mesh.ble.MeshDiagnostic
import com.nicobutter.beaconchat.mesh.ble.emit
import com.nicobutter.beaconchat.mesh.transport.MeshGattContract

/** Legacy direct chat plus opt-in experimental mesh. Hardware/session state is on the main looper. */
@Suppress("DEPRECATION", "MissingPermission")
class BLEMeshController(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter get() = manager.adapter
    private val handler = Handler(Looper.getMainLooper())
    private val serviceUuid = UUID.fromString("0000BEEF-0000-1000-8000-00805f9b34fb")
    private val chatUuid = UUID.fromString("0000C4A7-0000-1000-8000-00805f9b34fb")
    private val messageUuid = UUID.fromString("00004D51-0000-1000-8000-00805f9b34fb")
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var server: BluetoothGattServer? = null
    private var advertisingCallback: AdvertiseCallback? = null
    private var scanningCallback: ScanCallback? = null
    private var advertisedCallsign: String? = null
    private var advertisingGeneration = 0
    private var advertisingTimeout: Runnable? = null
    private var receiverRegistered = false
    private var activeSend: Send? = null
    val experimentalMesh = AndroidMeshTransport(this.context).also { mesh ->
        mesh.cancelServerConnection = { address ->
            try { adapter?.getRemoteDevice(address)?.let { server?.cancelConnection(it) } } catch (_: Exception) { }
        }
    }

    fun setMeshParticipation(enabled: Boolean) {
        if (!enabled || ready()) experimentalMesh.setEnabled(enabled)
    }

    private val _peers = MutableStateFlow<List<MeshPeer>>(emptyList())
    val peers = _peers.asStateFlow()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()
    private val _isStartingAdvertising = MutableStateFlow(false)
    val isStartingAdvertising = _isStartingAdvertising.asStateFlow()
    private val _scanRequested = MutableStateFlow(false)
    val scanRequested = _scanRequested.asStateFlow()
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled = _bluetoothEnabled.asStateFlow()
    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()
    private val _connectionStatus = MutableStateFlow("Sin conexión")
    val connectionStatus = _connectionStatus.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { updateBluetoothState() }
    }

    fun clearError() { _lastError.value = null }

    fun updateBluetoothState() {
        val enabled = try { adapter?.isEnabled == true } catch (_: SecurityException) { false }
        _bluetoothEnabled.value = enabled
        if (!enabled) {
            stop()
            _peers.value = emptyList()
        }
    }

    /** Monitor only while the Mesh screen is resumed. No background radio operations. */
    fun resume() {
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(context, receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
        }
        updateBluetoothState()
    }

    fun pause() {
        stop()
        if (receiverRegistered) {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    private fun ready(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= 31) listOf(
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ) else listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            _lastError.value = "Faltan permisos de Bluetooth/ubicación"
            return false
        }
        updateBluetoothState()
        if (!_bluetoothEnabled.value) _lastError.value = "Bluetooth desactivado o no disponible"
        return _bluetoothEnabled.value
    }

    fun startAdvertising(callsign: String) {
        try {
            val name = DirectBleProtocol.callsign(callsign)
            if (_isAdvertising.value || _isStartingAdvertising.value) {
                if (advertisedCallsign == name) return
                stopAdvertising()
            }
            if (!ready()) return
            advertiser = adapter?.bluetoothLeAdvertiser
                ?: throw IllegalStateException("Este dispositivo no permite publicidad BLE")
            advertisedCallsign = name
            _isStartingAdvertising.value = true
            MeshDebugLog.emit(MeshDiagnostic("advertising", status = "STARTING"))
            val generation = ++advertisingGeneration
            val callback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) { handler.post {
                    if (generation == advertisingGeneration) {
                        advertisingTimeout?.let(handler::removeCallbacks)
                        _isStartingAdvertising.value = false
                        _isAdvertising.value = true
                        MeshDebugLog.emit(MeshDiagnostic("advertising", status = "STARTED"))
                    }
                } }
                override fun onStartFailure(errorCode: Int) { handler.post {
                    if (generation == advertisingGeneration) {
                        MeshDebugLog.emit(MeshDiagnostic("advertising", status = "FAILED_$errorCode"))
                        advertisingFailed("Publicidad BLE falló: $errorCode")
                    }
                } }
            }
            advertisingCallback = callback
            advertisingTimeout = Runnable {
                if (generation == advertisingGeneration) advertisingFailed("Tiempo agotado al iniciar publicidad/GATT")
            }.also { handler.postDelayed(it, 10_000) }
            val serverCallback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) { handler.post {
                    if (generation == advertisingGeneration && (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS)) {
                        experimentalMesh.serverDisconnected(device.address)
                    } else if (generation == advertisingGeneration && newState == BluetoothProfile.STATE_CONNECTED) {
                        experimentalMesh.serverConnected(device.address)
                    }
                } }
                override fun onServiceAdded(status: Int, service: BluetoothGattService?) { handler.post {
                    if (generation != advertisingGeneration) return@post
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        advertisingFailed("No se pudo registrar servicio GATT: $status")
                        return@post
                    }
                    try {
                        // Primary packet: flags + UUID only (21 bytes worst case).
                        // Scan response: service data + callsign only (26 bytes worst case).
                        val data = AdvertiseData.Builder().addServiceUuid(ParcelUuid(serviceUuid)).build()
                        val response = AdvertiseData.Builder()
                            .addServiceData(ParcelUuid(serviceUuid), DirectBleProtocol.encodeCallsign(name)).build()
                        val settings = AdvertiseSettings.Builder().setConnectable(true).setTimeout(0)
                            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY).build()
                        advertiser!!.startAdvertising(settings, data, response, callback)
                    } catch (e: Exception) { advertisingFailed("No se pudo iniciar publicidad: ${e.message}") }
                } }

                override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int,
                    characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean,
                    responseNeeded: Boolean, offset: Int, value: ByteArray) { handler.post {
                    if (generation != advertisingGeneration) return@post
                    if (characteristic.uuid == MeshGattContract.CHARACTERISTIC_UUID || characteristic.uuid == MeshGattContract.IDENTITY_UUID) {
                        val meshStatus = when {
                            preparedWrite || !responseNeeded -> BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                            offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                            value.size !in 1..20 -> BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                            else -> try {
                                if (experimentalMesh.serverWrite(device.address, characteristic.uuid, value)) BluetoothGatt.GATT_SUCCESS
                                else BluetoothGatt.GATT_FAILURE
                            } catch (_: Exception) { BluetoothGatt.GATT_FAILURE }
                        }
                        MeshDebugLog.emit(MeshDiagnostic("server_operation",
                            phase = if (characteristic.uuid == MeshGattContract.IDENTITY_UUID) "WRITE_IDENTITY" else "WRITE_FRAME",
                            status = "GATT_$meshStatus"))
                        try { if (responseNeeded) server?.sendResponse(device, requestId, meshStatus, offset, null) }
                        catch (_: Exception) { _lastError.value = "No se pudo responder al canal mesh" }
                        return@post
                    }
                    var packet: DirectBleProtocol.Packet? = null
                    val status = when {
                        characteristic.uuid != messageUuid -> BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                        preparedWrite || !responseNeeded -> BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                        offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                        else -> try {
                            packet = DirectBleProtocol.decode(value)
                            BluetoothGatt.GATT_SUCCESS
                        } catch (_: Exception) { BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH }
                    }
                    try {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            val received = packet!!
                            // Android may use different central and advertising addresses.
                            // Associate only a unique observed callsign; this is not authentication.
                            val sender = _peers.value.firstOrNull { it.id == device.address }?.id
                                ?: _peers.value.singleOrNull { it.callsign == received.callsign }?.id
                                ?: device.address
                            _messages.update { it + ChatMessage(sender, received.callsign, "ME",
                                received.message, System.currentTimeMillis(), false) }
                        }
                        if (responseNeeded && server?.sendResponse(device, requestId, status, offset, null) != true) {
                            _lastError.value = "No se pudo responder a escritura GATT"
                        }
                    } catch (e: Exception) { _lastError.value = "Error de recepción: ${e.message}" }
                } }
                override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                    characteristic: BluetoothGattCharacteristic) { handler.post {
                    if (generation != advertisingGeneration) return@post
                    var response: ByteArray? = null
                    val status = when {
                        characteristic.uuid != MeshGattContract.CHARACTERISTIC_UUID && characteristic.uuid != MeshGattContract.IDENTITY_UUID ->
                            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                        offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                        else -> try {
                            response = experimentalMesh.serverRead(device.address, characteristic.uuid)
                            if (response != null) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                        } catch (_: Exception) { BluetoothGatt.GATT_FAILURE }
                    }
                    MeshDebugLog.emit(MeshDiagnostic("server_operation", phase = "READ", status = "GATT_$status"))
                    try { server?.sendResponse(device, requestId, status, offset, response) }
                    catch (_: Exception) { _lastError.value = "No se pudo responder a lectura mesh" }
                } }
                override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) { handler.post {
                    if (generation == advertisingGeneration) try {
                        server?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
                    } catch (_: Exception) { _lastError.value = "Error respondiendo GATT" }
                } }
            }
            server = manager.openGattServer(context, serverCallback)
                ?: throw IllegalStateException("Servidor GATT no disponible")
            val service = BluetoothGattService(chatUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            service.addCharacteristic(BluetoothGattCharacteristic(messageUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE))
            for (uuid in listOf(MeshGattContract.CHARACTERISTIC_UUID, MeshGattContract.IDENTITY_UUID)) {
                service.addCharacteristic(BluetoothGattCharacteristic(uuid,
                    BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE))
            }
            check(server!!.addService(service)) { "No se pudo iniciar registro GATT" }
        } catch (e: Exception) { advertisingFailed(e.message ?: "Error iniciando publicidad") }
    }

    private fun advertisingFailed(message: String) {
        stopAdvertising()
        experimentalMesh.setEnabled(false)
        _lastError.value = message
    }

    fun stopAdvertising() {
        ++advertisingGeneration
        advertisingTimeout?.let(handler::removeCallbacks)
        advertisingTimeout = null
        try { advertisingCallback?.let { advertiser?.stopAdvertising(it) } } catch (_: Exception) { }
        try { server?.close() } catch (_: Exception) { }
        experimentalMesh.serverStopped()
        advertisingCallback = null
        advertisedCallsign = null
        advertiser = null
        server = null
        _isAdvertising.value = false
        _isStartingAdvertising.value = false
        MeshDebugLog.emit(MeshDiagnostic("advertising", status = "STOPPED"))
    }

    fun startScanning() {
        if (scanningCallback != null || !ready()) return
        if (Build.VERSION.SDK_INT < 31) {
            val location = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!location.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !location.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                _lastError.value = "Activá Ubicación para escanear BLE en Android 6–11"
                return
            }
        }
        try {
            scanner = adapter?.bluetoothLeScanner ?: throw IllegalStateException("Escáner BLE no disponible")
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) { handler.post {
                    if (scanningCallback !== this) return@post
                    _isScanning.value = true // Android provides no onScanStarted callback.
                    if (_peers.value.isEmpty()) MeshDebugLog.emit(MeshDiagnostic("scan", status = "RESULT_RECEIVED"))
                    val bytes = result.scanRecord?.getServiceData(ParcelUuid(serviceUuid)) ?: return@post
                    val name = try { DirectBleProtocol.decodeCallsign(bytes) } catch (_: Exception) { return@post }
                    val peer = MeshPeer(result.device.address, "BeaconChat BLE", name, result.rssi, System.currentTimeMillis())
                    _peers.update { list -> list.filter { it.id != peer.id && System.currentTimeMillis() - it.lastSeen < 30_000 } + peer }
                    experimentalMesh.discovered(peer.id)
                } }
                override fun onScanFailed(errorCode: Int) { handler.post {
                    if (scanningCallback === this) {
                        stopScanning()
                        MeshDebugLog.emit(MeshDiagnostic("scan", status = "FAILED_$errorCode"))
                        _lastError.value = "Escaneo BLE falló: $errorCode"
                    }
                } }
            }
            scanningCallback = callback
            _scanRequested.value = true
            MeshDebugLog.emit(MeshDiagnostic("scan", status = "REQUESTED"))
            _lastError.value = null
            scanner!!.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUuid)).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        } catch (e: Exception) {
            stopScanning()
            _lastError.value = "No se pudo escanear: ${e.message}"
        }
    }

    fun stopScanning() {
        val callback = scanningCallback
        scanningCallback = null
        _scanRequested.value = false
        try { callback?.let { scanner?.stopScan(it) } } catch (_: Exception) { }
        scanner = null
        _isScanning.value = false
        _peers.value = emptyList()
        MeshDebugLog.emit(MeshDiagnostic("scan", status = "STOPPED"))
    }

    private class Send(val address: String, val name: String, val message: String, val payload: ByteArray) {
        var gatt: BluetoothGatt? = null
        var timeout: Runnable? = null
        val state = DirectSendState()
    }

    fun sendMessage(peerAddress: String, message: String, myCallsign: String) {
        if (activeSend != null || !ready()) return
        if (experimentalMesh.outgoingBusy) {
            _lastError.value = "Enlace mesh ocupado; esperá su finalización para enviar chat directo"
            return
        }
        val operation = try { Send(peerAddress, DirectBleProtocol.callsign(myCallsign), message,
            DirectBleProtocol.encode(myCallsign, message)) } catch (e: Exception) {
            _lastError.value = e.message ?: "Mensaje inválido"
            return
        }
        activeSend = operation
        experimentalMesh.setDirectBusy(true)
        _isSending.value = true
        _lastError.value = null
        _connectionStatus.value = "Conectando…"
        operation.timeout = Runnable { finish(operation, "Tiempo de envío agotado; recepción sin confirmar") }
            .also { handler.postDelayed(it, 20_000) }
        val callback = object : BluetoothGattCallback() {
            private fun dispatch(gatt: BluetoothGatt, action: () -> Unit) { handler.post {
                if (activeSend !== operation) { try { gatt.close() } catch (_: Exception) { }; return@post }
                try { action() } catch (e: Exception) { finish(operation, "Error GATT: ${e.message}") }
            } }
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) = dispatch(gatt) {
                if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                    finish(operation, "Conexión cerrada/error GATT: $status; recepción sin confirmar")
                } else if (newState == BluetoothProfile.STATE_CONNECTED && operation.state.connected()) {
                    _connectionStatus.value = "Descubriendo servicio…"
                    if (!gatt.discoverServices()) finish(operation, "No se pudo iniciar descubrimiento GATT")
                }
            }
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) = dispatch(gatt) {
                if (!operation.state.servicesDiscovered()) return@dispatch
                val characteristic = gatt.getService(chatUuid)?.getCharacteristic(messageUuid)
                if (status != BluetoothGatt.GATT_SUCCESS || characteristic == null ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
                    finish(operation, "Servicio/característica GATT no disponible: $status")
                } else {
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    characteristic.value = operation.payload
                    _connectionStatus.value = "Escribiendo…"
                    if (!gatt.writeCharacteristic(characteristic)) finish(operation, "No se pudo iniciar escritura GATT")
                }
            }
            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
                status: Int) = dispatch(gatt) {
                if (characteristic.uuid != messageUuid || operation.state.phase != DirectSendState.Phase.WRITING) return@dispatch
                if (status != BluetoothGatt.GATT_SUCCESS) finish(operation, "Escritura GATT falló: $status")
                else {
                    _messages.update { it + ChatMessage("ME", operation.name, operation.address,
                        operation.message, System.currentTimeMillis(), true) }
                    finish(operation, null)
                }
            }
        }
        try {
            operation.gatt = adapter?.getRemoteDevice(peerAddress)?.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            if (operation.gatt == null) finish(operation, "No se pudo iniciar conexión GATT")
        } catch (e: Exception) { finish(operation, "No se pudo conectar: ${e.message}") }
    }

    private fun finish(operation: Send, error: String?) {
        if (activeSend !== operation || !operation.state.finish()) return
        activeSend = null
        operation.timeout?.let(handler::removeCallbacks)
        try { operation.gatt?.disconnect() } catch (_: Exception) { }
        try { operation.gatt?.close() } catch (_: Exception) { }
        experimentalMesh.setDirectBusy(false)
        _isSending.value = false
        _lastError.value = error
        _connectionStatus.value = if (error == null) "Escritura GATT confirmada (sin ACK de aplicación)" else "Sin conexión"
    }

    fun cancelSend() { activeSend?.let { finish(it, "Envío cancelado; recepción sin confirmar") } }
    fun stop() { experimentalMesh.setEnabled(false); cancelSend(); stopScanning(); stopAdvertising() }
    fun cleanup() { pause() }
}
