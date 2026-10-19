package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import java.util.ArrayDeque
import java.util.UUID

enum class MeshActivityKind { ORIGINATED, LOCAL_RECEIVED, QUEUED, LINK_WRITTEN, FAILURE, INFO }
data class MeshActivity(val kind: MeshActivityKind, val detail: String, val message: MeshMessage? = null)
data class MeshNeighbor(val address: String, val nodeId: NodeId?, val lastSeen: Long,
    val lastProbe: Long?, val state: String)
data class MeshBleSnapshot(val nodeId: NodeId?, val enabled: Boolean = false, val neighbors: List<MeshNeighbor> = emptyList(),
    val events: List<MeshActivity> = emptyList(), val queued: Int = 0, val phase: String = "IDLE", val incoming: Int = 0)

/**
 * The actual adapter policy shared by Android and fake GATT tests. All calls are serialized by host.
 * Bounds: 16 discovered radio handles, 8 jobs INCLUDING active, 8 incoming sessions, 32 history events.
 * The engine is owned once per local logical node, not per Bluetooth address or connection.
 */
class MeshBleNode(val nodeId: NodeId, private val clock: MeshClock, port: MeshGattPort,
    private val changed: (MeshBleSnapshot) -> Unit = {},
    private val closeIncomingLink: (MeshLinkSession) -> Unit = {}) {
    private val engine = MeshEngine(nodeId, clock)
    private val reassembler = MeshReassembler(clock)
    private val neighbors = linkedMapOf<String, MeshNeighbor>()
    private val queue = ArrayDeque<MeshGattJob>()
    private val events = ArrayDeque<MeshActivity>()
    private val incoming = linkedMapOf<MeshLinkSession, Incoming>()
    private class Incoming(val startedAt: Long, var touchedAt: Long, var identity: NodeId? = null, var receipt: ByteArray? = null)
    private var enabled = false
    private var directBusy = false
    private var nextToken = 0L
    private val transferIds = MeshTransferIds()
    val client = MeshGattClient(nodeId, clock, port, ::linkFinished)

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (!value) {
            queue.forEach { if (it.message != null) event(MeshActivityKind.FAILURE, "Cola mesh cancelada; destino sin confirmar", it.message) }
            queue.clear()
            client.cancel()
            incoming.keys.toList().forEach { closeIncoming(it); closeIncomingLink(it) }
            neighbors.clear()
        }
        event(MeshActivityKind.INFO, if (value) "Mesh experimental activo; identidades sin autenticar" else "Participación mesh desactivada")
        publish()
    }

    fun setDirectBusy(value: Boolean) { directBusy = value; pump(); publish() }

    /** Address is only a temporary radio handle. Capability and NodeId come from the GATT hello. */
    fun discovered(address: String) {
        if (!enabled || address.length > 64) return
        val old = neighbors[address]
        if (old == null && neighbors.size >= 16) return
        neighbors[address] = old?.copy(lastSeen = clock.nowMillis())
            ?: MeshNeighbor(address, null, clock.nowMillis(), null, "Capacidad mesh pendiente")
        probe(address)
        pump()
        publish()
    }

    fun originate(destination: NodeId?, content: String, ttl: Int = 3, messageId: MessageId = MessageId(UUID.randomUUID())): Boolean {
        if (!enabled) { event(MeshActivityKind.FAILURE, "Mesh desactivado"); publish(); return false }
        val candidate = MeshMessage(messageId, nodeId, destination, content, ttl)
        try { MeshMessageCodec.encode(candidate) } catch (e: IllegalArgumentException) {
            event(MeshActivityKind.FAILURE, e.message ?: "Mensaje inválido"); publish(); return false
        }
        val decision = engine.originate(destination, content, ttl, messageId)
        if (decision.acceptance != Acceptance.ACCEPTED) {
            event(MeshActivityKind.FAILURE, "Origen rechazado: ${decision.acceptance}", candidate)
            publish(); return false
        }
        event(MeshActivityKind.ORIGINATED, "Originado localmente; recepción remota sin confirmar", decision.message)
        recordForwarding(decision)
        dispatch()
        publish()
        return true
    }

    fun openIncoming(session: MeshLinkSession): Boolean {
        if (!enabled) return false
        if (session in incoming) return true
        if (incoming.size >= 8) return false
        incoming[session] = Incoming(clock.nowMillis(), clock.nowMillis())
        publish()
        return true
    }

    fun readIdentity(session: MeshLinkSession): ByteArray? =
        if (enabled && session in incoming) MeshLinkProtocol.hello(nodeId) else null

    fun acceptIdentity(session: MeshLinkSession, bytes: ByteArray): Boolean {
        val connection = incoming[session] ?: return false
        if (!enabled) return false
        val declared = try { MeshLinkProtocol.identity(bytes) } catch (_: IllegalArgumentException) { return false }
        if (declared == nodeId || (connection.identity != null && connection.identity != declared)) {
            closeIncoming(session)
            closeIncomingLink(session)
            event(MeshActivityKind.FAILURE, "Identidad propia o cambio de identidad en sesión entrante")
            publish(); return false
        }
        connection.identity = declared
        connection.touchedAt = clock.nowMillis()
        return true
    }

    fun acceptFrame(session: MeshLinkSession, bytes: ByteArray): Boolean {
        val connection = incoming[session] ?: return false
        val previousHop = connection.identity ?: return false
        if (!enabled) return false
        connection.touchedAt = clock.nowMillis()
        val result = reassembler.accept(session, bytes)
        if (result is ReassemblyResult.Rejected) {
            event(MeshActivityKind.FAILURE, "Fragmento rechazado: ${result.reason}")
            publish(); return false
        }
        if (result is ReassemblyResult.Complete) {
            val message = try { MeshMessageCodec.decode(result.payload) } catch (e: IllegalArgumentException) {
                event(MeshActivityKind.FAILURE, "Envelope rechazado: ${e.message}")
                publish(); return false
            }
            val decision = engine.receive(message, previousHop)
            if (decision.deliverLocally) event(MeshActivityKind.LOCAL_RECEIVED, "Recibido localmente; no confirma otros destinos", message)
            if (decision.acceptance == Acceptance.ACCEPTED) recordForwarding(decision)
            else if (decision.acceptance !in listOf(Acceptance.DUPLICATE, Acceptance.OWN_ORIGIN)) {
                event(MeshActivityKind.FAILURE, "Motor rechazó: ${decision.acceptance}", message)
                publish(); return false
            }
            dispatch()
        }
        connection.receipt = MeshLinkProtocol.receipt(bytes)
        publish()
        return true
    }

    fun readReceipt(session: MeshLinkSession): ByteArray? = incoming[session]?.receipt?.copyOf()

    fun closeIncoming(session: MeshLinkSession) {
        incoming.remove(session)
        reassembler.discardSession(session)
        publish()
    }

    fun tick() {
        if (!enabled) return
        val now = clock.nowMillis()
        client.tick()
        val expired = reassembler.cleanup()
        if (expired > 0) event(MeshActivityKind.INFO, "Expiraron $expired transferencias/registros de reensamblado; sin entrega parcial")
        incoming.filter { now - it.value.touchedAt >= 20_000 || now - it.value.startedAt >= 30_000 }
            .keys.toList().forEach { closeIncoming(it); closeIncomingLink(it) }
        neighbors.entries.removeAll { now - it.value.lastSeen >= 30_000 }
        neighbors.keys.toList().forEach(::probe)
        engine.cleanup()
        pump()
        publish()
    }

    fun snapshot() = MeshBleSnapshot(nodeId, enabled, neighbors.values.toList(), events.toList(),
        queue.size + if (client.busy) 1 else 0, client.phase.name, incoming.size)
    fun reassemblySnapshot() = reassembler.snapshot()

    private fun probe(address: String) {
        val neighbor = neighbors[address] ?: return
        if (clock.nowMillis() - neighbor.lastSeen >= 30_000 ||
            neighbor.lastProbe?.let { clock.nowMillis() - it < 30_000 } == true ||
            queue.any { it.address == address } || client.job?.address == address ||
            queue.count { it.message == null } >= 2 || usedJobs() >= 8) return
        neighbors[address] = neighbor.copy(lastProbe = clock.nowMillis(), state = "Verificando características e identidad…")
        queue.addLast(MeshGattJob(++nextToken, address, null, null, null, 0, clock.nowMillis()))
    }

    private fun dispatch() {
        val results = engine.dispatch(MeshTransport { message, excluded ->
            val targets = neighbors.values.filter { it.nodeId != null && it.nodeId !in excluded &&
                clock.nowMillis() - it.lastSeen < 30_000 }.distinctBy { it.nodeId }
            if (targets.isEmpty()) {
                event(MeshActivityKind.FAILURE, "Sin vecinos mesh habilitados para retransmitir; destino sin confirmar", message)
                Handoff.UNAVAILABLE
            } else if (targets.size > 8 - usedJobs()) {
                event(MeshActivityKind.FAILURE, "Cola mesh llena; fanout rechazado sin admisión parcial", message)
                Handoff.UNAVAILABLE
            } else {
                val encoded = MeshMessageCodec.encode(message)
                val ids = targets.map { transferIds.allocate(clock.nowMillis()) }
                targets.forEachIndexed { index, target ->
                    queue.addLast(MeshGattJob(++nextToken, target.address, target.nodeId, message, encoded, ids[index], clock.nowMillis()))
                }
                event(MeshActivityKind.QUEUED, "Admitido en cola local hacia ${targets.size} vecino(s); destino sin confirmar", message)
                Handoff.ACCEPTED
            }
        })
        results.filter { it.status == DispatchStatus.TRANSPORT_ERROR }.forEach {
            event(MeshActivityKind.FAILURE, "Error al admitir transporte; destino sin confirmar", it.message)
        }
        pump()
    }

    private fun recordForwarding(decision: MeshDecision) {
        if (decision.forwarding == Forwarding.TTL_EXHAUSTED) event(MeshActivityKind.INFO, "TTL agotado; no se retransmite", decision.message)
        if (decision.forwarding == Forwarding.QUEUE_FULL) event(MeshActivityKind.FAILURE, "Cola del motor llena", decision.message)
    }

    private fun usedJobs() = queue.size + if (client.busy) 1 else 0
    private fun pump() {
        while (queue.peekFirst()?.let { clock.nowMillis() - it.enqueuedAt >= 30_000 } == true) {
            val expired = queue.removeFirst()
            event(MeshActivityKind.FAILURE, "Venció espera en cola mesh; destino sin confirmar", expired.message)
        }
        if (enabled && !directBusy && !client.busy) queue.pollFirst()?.let(client::start)
    }

    private fun linkFinished(result: MeshLinkResult) {
        neighbors[result.job.address]?.let {
            val success = result.outcome == MeshLinkOutcome.PROBED || result.outcome == MeshLinkOutcome.LINK_WRITTEN
            neighbors[result.job.address] = it.copy(nodeId = if (success) result.remote else null,
                state = if (success) "Mesh V1; identidad declarada sin autenticar" else result.detail)
        }
        if (result.outcome == MeshLinkOutcome.LINK_WRITTEN) event(MeshActivityKind.LINK_WRITTEN, result.detail, result.job.message)
        else if (result.outcome != MeshLinkOutcome.PROBED) event(MeshActivityKind.FAILURE, result.detail, result.job.message)
        // Continue capability checks already discovered while the bounded probe queue was full.
        if (enabled) neighbors.keys.toList().forEach(::probe)
        pump()
        publish()
    }

    private fun event(kind: MeshActivityKind, detail: String, message: MeshMessage? = null) {
        if (events.size == 32) events.removeFirst()
        events.addLast(MeshActivity(kind, detail, message))
    }
    private fun publish() = changed(snapshot())
}
