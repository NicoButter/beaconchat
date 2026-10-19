package com.nicobutter.beaconchat.mesh.ble

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.nicobutter.beaconchat.mesh.core.MeshClock
import com.nicobutter.beaconchat.mesh.core.NodeId
import com.nicobutter.beaconchat.mesh.transport.MeshGattContract
import com.nicobutter.beaconchat.mesh.transport.MeshLinkSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import com.nicobutter.beaconchat.sos.SosIncident
import com.nicobutter.beaconchat.sos.SosMeshGateway

/** Android I/O only. Node/client policy is shared with JVM tests; all Bluetooth state is on main. */
@Suppress("DEPRECATION", "MissingPermission")
class AndroidMeshTransport(context: Context) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val clock = MeshClock { SystemClock.elapsedRealtime() }
    private val manager = this.context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val _state = MutableStateFlow(MeshBleSnapshot(null))
    val state = _state.asStateFlow()
    private var node: MeshBleNode? = null
    private var directBusy = false
    private var activeGatt: BluetoothGatt? = null
    private var activeToken: Long? = null
    private val incoming = linkedMapOf<String, MeshLinkSession>()
    var cancelServerConnection: (String) -> Unit = {}
    val outgoingBusy get() = node?.client?.busy == true
    private val ticker = object : Runnable {
        override fun run() {
            node?.tick()
            if (_state.value.enabled) handler.postDelayed(this, 1_000)
        }
    }

    init {
        // Disk reads/commit happen on IO, and identity is exposed only after successful persistence.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = this@AndroidMeshTransport.context.getSharedPreferences("mesh_identity_v1", Context.MODE_PRIVATE)
                val saved = preferences.getString("node_id", null)
                val identity = if (saved == null) UUID.randomUUID() else UUID.fromString(saved)
                check(identity != UUID(0, 0)) { "Identidad mesh persistida inválida" }
                if (saved == null) check(preferences.edit().putString("node_id", identity.toString()).commit()) {
                    "No se pudo persistir NodeId"
                }
                handler.post {
                    node = MeshBleNode(NodeId(identity), clock, MeshGattPort(::execute), { _state.value = it }, ::expireIncoming)
                    node!!.setDirectBusy(directBusy)
                }
            } catch (e: Exception) {
                handler.post { _state.value = MeshBleSnapshot(null, events = listOf(MeshActivity(
                    MeshActivityKind.FAILURE, "Inicialización mesh: ${e.message}"))) }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        node?.setEnabled(value)
        handler.removeCallbacks(ticker)
        if (value && _state.value.enabled) handler.postDelayed(ticker, 1_000)
    }
    fun setDirectBusy(value: Boolean) { directBusy = value; node?.setDirectBusy(value) }
    fun discovered(address: String) { node?.discovered(address) }
    fun originate(destination: NodeId?, content: String, ttl: Int) = node?.originate(destination, content, ttl) == true
    fun submitSosLaboratory(incident: SosIncident) = SosMeshGateway { content -> originate(null, content, 3) }.submit(incident)

    private fun serverSession(address: String): MeshLinkSession? {
        if (!_state.value.enabled) return null
        incoming[address]?.let { return it }
        if (incoming.size >= 8) return null
        val session = MeshLinkSession(UUID.randomUUID())
        if (node?.openIncoming(session) != true) return null
        incoming[address] = session
        return session
    }
    fun serverWrite(address: String, characteristic: UUID, value: ByteArray): Boolean {
        val session = if (characteristic == MeshGattContract.IDENTITY_UUID) serverSession(address) else incoming[address]
        session ?: return false
        return when (characteristic) {
            MeshGattContract.IDENTITY_UUID -> node?.acceptIdentity(session, value) == true
            MeshGattContract.CHARACTERISTIC_UUID -> node?.acceptFrame(session, value) == true
            else -> false
        }
    }
    fun serverRead(address: String, characteristic: UUID): ByteArray? {
        val session = if (characteristic == MeshGattContract.IDENTITY_UUID) serverSession(address) else incoming[address]
        session ?: return null
        return when (characteristic) {
            MeshGattContract.IDENTITY_UUID -> node?.readIdentity(session)
            MeshGattContract.CHARACTERISTIC_UUID -> node?.readReceipt(session)
            else -> null
        }
    }
    fun serverDisconnected(address: String) { incoming.remove(address)?.let { node?.closeIncoming(it) } }
    fun serverStopped() { incoming.values.toList().forEach { node?.closeIncoming(it) }; incoming.clear() }
    private fun expireIncoming(session: MeshLinkSession) {
        val address = incoming.entries.firstOrNull { it.value == session }?.key ?: return
        incoming.remove(address)
        cancelServerConnection(address)
    }

    private fun execute(command: MeshGattCommand): Boolean {
        if (command is MeshGattCommand.Close) {
            if (activeToken == command.token) {
                val old = activeGatt
                activeGatt = null
                activeToken = null
                try { old?.disconnect() } catch (_: Exception) { }
                try { old?.close() } catch (_: Exception) { }
            }
            return true
        }
        if (command is MeshGattCommand.Connect) {
            if (activeGatt != null) return false
            activeToken = command.token
            activeGatt = manager.adapter?.getRemoteDevice(command.address)?.connectGatt(
                context, false, callback(command.token), BluetoothDevice.TRANSPORT_LE)
            return activeGatt != null
        }
        if (activeToken != command.token) return false
        val gatt = activeGatt ?: return false
        val service = gatt.getService(MeshGattContract.SERVICE_UUID)
        return when (command) {
            is MeshGattCommand.Discover -> gatt.discoverServices()
            is MeshGattCommand.ReadIdentity -> service?.getCharacteristic(MeshGattContract.IDENTITY_UUID)?.let(gatt::readCharacteristic) == true
            is MeshGattCommand.ReadReceipt -> service?.getCharacteristic(MeshGattContract.CHARACTERISTIC_UUID)?.let(gatt::readCharacteristic) == true
            is MeshGattCommand.WriteIdentity -> write(gatt, service?.getCharacteristic(MeshGattContract.IDENTITY_UUID), command.value)
            is MeshGattCommand.WriteFrame -> write(gatt, service?.getCharacteristic(MeshGattContract.CHARACTERISTIC_UUID), command.value)
            else -> false
        }
    }

    private fun write(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?, value: ByteArray): Boolean {
        characteristic ?: return false
        return if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = value
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun callback(token: Long) = object : BluetoothGattCallback() {
        private fun dispatch(gatt: BluetoothGatt, action: (MeshGattClient) -> Unit) { handler.post {
            if (activeToken != token || activeGatt !== gatt) {
                try { gatt.close() } catch (_: Exception) { }
                return@post
            }
            val client = node?.client ?: return@post
            try { action(client) } catch (_: Exception) { client.disconnected(token) }
            node?.let { _state.value = it.snapshot() }
        } }
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) = dispatch(gatt) {
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) it.disconnected(token)
            else if (newState == BluetoothProfile.STATE_CONNECTED) it.connected(token, true)
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) = dispatch(gatt) {
            val service = gatt.getService(MeshGattContract.SERVICE_UUID)
            val identity = service?.getCharacteristic(MeshGattContract.IDENTITY_UUID)
            val data = service?.getCharacteristic(MeshGattContract.CHARACTERISTIC_UUID)
            val flags = BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE
            it.services(token, status == BluetoothGatt.GATT_SUCCESS && identity != null && data != null &&
                identity.properties and flags == flags && data.properties and flags == flags)
        }
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) = dispatch(gatt) {
            when (characteristic.uuid) {
                MeshGattContract.IDENTITY_UUID -> it.identityWritten(token, status == BluetoothGatt.GATT_SUCCESS)
                MeshGattContract.CHARACTERISTIC_UUID -> it.frameWritten(token, status == BluetoothGatt.GATT_SUCCESS)
            }
        }
        private fun read(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            val copy = if (value.size <= 20) value.copyOf() else byteArrayOf()
            dispatch(gatt) {
                when (characteristic.uuid) {
                    MeshGattContract.IDENTITY_UUID -> it.identityRead(token, copy, status == BluetoothGatt.GATT_SUCCESS)
                    MeshGattContract.CHARACTERISTIC_UUID -> it.receiptRead(token, copy, status == BluetoothGatt.GATT_SUCCESS)
                }
            }
        }
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) =
            read(gatt, characteristic, value, status)
        @Deprecated("Legacy callback for Android API 24–32")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33) read(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
        }
    }
}
