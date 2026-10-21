package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.*
import com.nicobutter.beaconchat.mesh.transport.*
import java.util.ArrayDeque
import java.util.UUID
import com.nicobutter.beaconchat.sos.SosMeshPolicy

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
    private val closeIncomingLink: (MeshLinkSession) -> Unit = {},
    private val acceptedInbound: (MeshMessage) -> Unit = {},
    private val diagnostics: MeshDiagnostics = MeshDiagnostics.NONE) {
    private val engine = MeshEngine(nodeId, clock)
    private val reassembler = MeshReassembler(clock, ReassemblyLimits(retentionMillis = MeshBleTiming.REASSEMBLY_MILLIS))
    private val neighbors = linkedMapOf<String, MeshNeighbor>()
    private val queue = ArrayDeque<MeshGattJob>()
    private val events = ArrayDeque<MeshActivity>()
    private val incoming = linkedMapOf<MeshLinkSession, Incoming>()
    private class Incoming(val startedAt: Long, var touchedAt: Long, var identity: NodeId? = null, var receipt: ByteArray? = null) {
        val transfers = linkedMapOf<Int, Long>()
    }
    private var enabled = false
    private var directBusy = false
    private var nextToken = 0L
    private val transferIds = MeshTransferIds()
    val client = MeshGattClient(nodeId, clock, port, ::linkFinished, diagnostics)

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

    fun setDirectBusy(value: Boolean) {
        directBusy = value
        diagnostics.emit(MeshDiagnostic("direct_link", status = if (value) "BUSY" else "RELEASED"))
        pump(); publish()
    }

    /** Address is only a temporary radio handle. Capability and NodeId come from the GATT hello. */
    fun discovered(address: String) {
        if (!enabled || address.length > 64) return
        val old = neighbors[address]
        if (old == null && neighbors.size >= 16) return
        neighbors[address] = old?.copy(lastSeen = clock.nowMillis())
            ?: MeshNeighbor(address, null, clock.nowMillis(), null, "Capacidad mesh pendiente")
        if (old == null) diagnostics.emit(MeshDiagnostic("neighbor_discovered", peer = address))
        probe(address)
        pump()
        publish()
    }

    fun originate(destination: NodeId?, content: String, ttl: Int = 3, messageId: MessageId = MessageId(UUID.randomUUID())): Boolean {
        if (!enabled) { event(MeshActivityKind.FAILURE, "Mesh desactivado"); publish(); return false }
        if (!SosMeshPolicy.contentPermitted(content)) {
            event(MeshActivityKind.FAILURE, "SOS bloqueado: solo ensayos sintéticos o sin posición; no se transmite GNSS real")
            publish(); return false
        }
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
        diagnostics.emit(MeshDiagnostic("incoming_open", session = session))
        publish()
        return true
    }

    fun readIdentity(session: MeshLinkSession): ByteArray? =
        if (enabled && liveIncoming(session) != null) MeshLinkProtocol.hello(nodeId) else null

    fun acceptIdentity(session: MeshLinkSession, bytes: ByteArray): Boolean {
        val connection = liveIncoming(session) ?: return false
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
        diagnostics.emit(MeshDiagnostic("incoming_identity", nodeId = declared, session = session, status = "VERIFIED_UNAUTHENTICATED"))
        return true
    }

    fun acceptFrame(session: MeshLinkSession, bytes: ByteArray): Boolean {
        val connection = liveIncoming(session) ?: return false
        val previousHop = connection.identity ?: return false
        if (!enabled) return false
        val header = try { MeshFragmentProtocol.parse(bytes) } catch (_: IllegalArgumentException) { null }
        val result = reassembler.accept(session, bytes)
        val now = clock.nowMillis()
        connection.transfers.entries.removeAll { now - it.value >= MeshBleTiming.REASSEMBLY_MILLIS }
        val transferStart = header?.let { connection.transfers[it.transferId] ?: now }
        if (header != null) {
            if (result is ReassemblyResult.Pending) connection.transfers.putIfAbsent(header.transferId, now)
            if (result is ReassemblyResult.Complete || result is ReassemblyResult.Rejected) connection.transfers.remove(header.transferId)
        }
        diagnostics.emit(MeshDiagnostic("incoming_fragment", frameIndex = header?.index, frameCount = header?.count,
            transferId = header?.transferId, session = session,
            durationMillis = if (result is ReassemblyResult.Complete) now - transferStart!! else null,
            status = when (result) {
                is ReassemblyResult.Rejected -> result.reason.name
                is ReassemblyResult.Complete -> "COMPLETE"
                is ReassemblyResult.Duplicate -> "DUPLICATE"
                is ReassemblyResult.Pending -> "ADMITTED"
            }))
        if (result is ReassemblyResult.Rejected) {
            event(MeshActivityKind.FAILURE, "Fragmento rechazado: ${result.reason}")
            publish(); return false
        }
        if (result is ReassemblyResult.Complete) {
            val message = try { MeshMessageCodec.decode(result.payload) } catch (e: IllegalArgumentException) {
                event(MeshActivityKind.FAILURE, "Envelope rechazado: ${e.message}")
                publish(); return false
            }
            if (!SosMeshPolicy.contentPermitted(message.content)) {
                event(MeshActivityKind.FAILURE, "SOS entrante bloqueado por privacidad/formato; no se entrega ni retransmite")
                publish(); return false
            }
            val decision = engine.receive(message, previousHop)
            diagnostics.emit(MeshDiagnostic("mesh_receive", messageId = message.messageId, ttl = message.hopsRemaining,
                nodeId = previousHop, session = session, status = decision.acceptance.name))
            if (decision.acceptance == Acceptance.ACCEPTED) {
                // Independent of logical destination, visual history and LOCAL_RECEIVED events.
                // An optional observer must not interrupt normal mesh forwarding or link receipts.
                try { acceptedInbound(message) } catch (_: Exception) {
                    event(MeshActivityKind.FAILURE, "Observador de recepción no disponible")
                }
            }
            if (decision.deliverLocally) event(MeshActivityKind.LOCAL_RECEIVED, "Recibido localmente; no confirma otros destinos", message)
            if (decision.acceptance == Acceptance.ACCEPTED) recordForwarding(decision)
            else if (decision.acceptance !in listOf(Acceptance.DUPLICATE, Acceptance.OWN_ORIGIN)) {
                event(MeshActivityKind.FAILURE, "Motor rechazó: ${decision.acceptance}", message)
                publish(); return false
            }
            dispatch()
        }
        connection.touchedAt = clock.nowMillis()
        connection.receipt = MeshLinkProtocol.receipt(bytes)
        publish()
        return true
    }

    fun readReceipt(session: MeshLinkSession): ByteArray? = liveIncoming(session)?.receipt?.copyOf()

    fun closeIncoming(session: MeshLinkSession) {
        incoming.remove(session)
        reassembler.discardSession(session)
        diagnostics.emit(MeshDiagnostic("incoming_closed", session = session))
        publish()
    }

    fun tick() {
        if (!enabled) return
        val now = clock.nowMillis()
        client.tick()
        val expired = reassembler.cleanup()
        if (expired > 0) event(MeshActivityKind.INFO, "Expiraron $expired transferencias/registros de reensamblado; sin entrega parcial")
        incoming.keys.toList().forEach(::liveIncoming)
        neighbors.entries.removeAll {
            (now - it.value.lastSeen >= MeshBleTiming.NEIGHBOR_MILLIS).also { expiredNeighbor ->
                if (expiredNeighbor) diagnostics.emit(MeshDiagnostic("neighbor_expired", peer = it.key, nodeId = it.value.nodeId))
            }
        }
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
        if (clock.nowMillis() - neighbor.lastSeen >= MeshBleTiming.NEIGHBOR_MILLIS ||
            neighbor.lastProbe?.let { clock.nowMillis() - it < MeshBleTiming.NEIGHBOR_MILLIS } == true ||
            queue.any { it.address == address } || client.job?.address == address ||
            queue.count { it.message == null } >= 2 || usedJobs() >= 8) return
        neighbors[address] = neighbor.copy(lastProbe = clock.nowMillis(), state = "Verificando características e identidad…")
        queue.addLast(MeshGattJob(++nextToken, address, null, null, null, 0, clock.nowMillis()))
    }

    private fun dispatch() {
        val results = engine.dispatch(MeshTransport { message, excluded ->
            val targets = neighbors.values.filter { it.nodeId != null && it.nodeId !in excluded &&
                clock.nowMillis() - it.lastSeen < MeshBleTiming.NEIGHBOR_MILLIS }.distinctBy { it.nodeId }
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
        diagnostics.emit(MeshDiagnostic("mesh_forward", messageId = decision.message.messageId,
            ttl = decision.message.hopsRemaining, status = decision.forwarding.name))
        if (decision.forwarding == Forwarding.TTL_EXHAUSTED) event(MeshActivityKind.INFO, "TTL agotado; no se retransmite", decision.message)
        if (decision.forwarding == Forwarding.QUEUE_FULL) event(MeshActivityKind.FAILURE, "Cola del motor llena", decision.message)
    }

    private fun usedJobs() = queue.size + if (client.busy) 1 else 0
    private fun pump() {
        while (queue.peekFirst()?.let { clock.nowMillis() - it.enqueuedAt >= MeshBleTiming.QUEUE_MILLIS } == true) {
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
        diagnostics.emit(MeshDiagnostic("mesh_activity", messageId = message?.messageId,
            ttl = message?.hopsRemaining, status = kind.name))
        if (events.size == 32) events.removeFirst()
        events.addLast(MeshActivity(kind, detail, message))
    }
    private fun liveIncoming(session: MeshLinkSession): Incoming? {
        val connection = incoming[session] ?: return null
        val now = clock.nowMillis()
        if (now - connection.touchedAt >= MeshBleTiming.INCOMING_IDLE_MILLIS ||
            now - connection.startedAt >= MeshBleTiming.INCOMING_SESSION_MILLIS) {
            diagnostics.emit(MeshDiagnostic("incoming_expired", session = session, durationMillis = now - connection.startedAt))
            closeIncoming(session)
            closeIncomingLink(session)
            return null
        }
        return connection
    }
    private fun publish() = changed(snapshot())
}
