package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.MeshClock
import com.nicobutter.beaconchat.mesh.core.MeshMessage
import com.nicobutter.beaconchat.mesh.core.NodeId
import com.nicobutter.beaconchat.mesh.transport.MeshFragmentProtocol

/** One asynchronous operation at a time. Implementations enqueue callbacks; they must not block. */
fun interface MeshGattPort { fun execute(command: MeshGattCommand): Boolean }

sealed class MeshGattCommand(val token: Long) {
    class Connect(token: Long, val address: String) : MeshGattCommand(token)
    class Discover(token: Long) : MeshGattCommand(token)
    class ReadIdentity(token: Long) : MeshGattCommand(token)
    class WriteIdentity(token: Long, val value: ByteArray) : MeshGattCommand(token)
    class WriteFrame(token: Long, val value: ByteArray) : MeshGattCommand(token)
    class ReadReceipt(token: Long) : MeshGattCommand(token)
    class Close(token: Long) : MeshGattCommand(token)
}

data class MeshGattJob(val token: Long, val address: String, val expectedNode: NodeId?,
    val message: MeshMessage?, val encoded: ByteArray?, val transferId: Int, val enqueuedAt: Long = 0)
enum class MeshLinkOutcome { PROBED, LINK_WRITTEN, FAILED, CANCELLED }
data class MeshLinkResult(val job: MeshGattJob, val outcome: MeshLinkOutcome, val remote: NodeId?, val detail: String)

/**
 * Hardware-independent client used by both the Android bridge and JVM fake GATT.
 * Android write callbacks have no request ID. A read-back receipt correlates transfer/index/CRC
 * before advancing, so a late/duplicated callback cannot silently skip a fragment.
 */
class MeshGattClient(private val local: NodeId, private val clock: MeshClock, private val port: MeshGattPort,
    private val finished: (MeshLinkResult) -> Unit, private val diagnostics: MeshDiagnostics = MeshDiagnostics.NONE) {
    enum class Phase { IDLE, CONNECTING, DISCOVERING, READING_IDENTITY, WRITING_IDENTITY, WRITING_FRAME, READING_RECEIPT }
    var phase = Phase.IDLE; private set
    var job: MeshGattJob? = null; private set
    private var remote: NodeId? = null
    private var frames: List<ByteArray> = emptyList()
    private var index = 0
    private var startedAt = 0L
    private var operationAt = 0L
    private var transferAt: Long? = null
    private var transferLimit = MeshBleTiming.LEGACY_TRANSFER_MILLIS
    val busy get() = job != null

    fun start(next: MeshGattJob) {
        check(!busy)
        job = next
        remote = null
        frames = next.encoded?.let { MeshFragmentProtocol.fragment(it, next.transferId) } ?: emptyList()
        transferLimit = if (frames.isEmpty()) MeshBleTiming.LEGACY_TRANSFER_MILLIS else MeshBleTiming.transferMillis(frames.size)
        index = 0
        startedAt = clock.nowMillis()
        transferAt = null
        issue(Phase.CONNECTING, MeshGattCommand.Connect(next.token, next.address))
    }

    fun connected(token: Long, success: Boolean) {
        if (!matches(token, Phase.CONNECTING)) return
        measured(success)
        if (!success) fail("Conexión mesh fallida")
        else issue(Phase.DISCOVERING, MeshGattCommand.Discover(token))
    }

    fun services(token: Long, supported: Boolean) {
        if (!matches(token, Phase.DISCOVERING)) return
        measured(supported)
        if (!supported) fail("Vecino sin características/capacidades mesh")
        else issue(Phase.READING_IDENTITY, MeshGattCommand.ReadIdentity(token))
    }

    fun identityRead(token: Long, value: ByteArray, success: Boolean) {
        if (!matches(token, Phase.READING_IDENTITY)) return
        measured(success)
        if (!success) { fail("No se pudo leer identidad mesh"); return }
        val declared = try { MeshLinkProtocol.identity(value) } catch (e: IllegalArgumentException) {
            fail(e.message ?: "Identidad mesh inválida"); return
        }
        if (declared == local || (job!!.expectedNode != null && job!!.expectedNode != declared)) {
            fail("Identidad mesh propia o cambiada durante la sesión"); return
        }
        remote = declared
        diagnostics.emit(MeshDiagnostic("identity_verified", token, nodeId = declared, peer = job!!.address))
        issue(Phase.WRITING_IDENTITY, MeshGattCommand.WriteIdentity(token, MeshLinkProtocol.hello(local)))
    }

    fun identityWritten(token: Long, success: Boolean) {
        if (!matches(token, Phase.WRITING_IDENTITY)) return
        measured(success)
        if (!success) fail("El vecino rechazó nuestra identidad mesh")
        else if (frames.isEmpty()) finish(MeshLinkOutcome.PROBED, "Capacidad mesh verificada; identidad declarada sin autenticar")
        else { transferAt = clock.nowMillis(); writeFrame() }
    }

    fun frameWritten(token: Long, success: Boolean) {
        if (!matches(token, Phase.WRITING_FRAME)) return
        measured(success)
        if (!success) fail("Escritura de fragmento mesh fallida; destino sin confirmar")
        else issue(Phase.READING_RECEIPT, MeshGattCommand.ReadReceipt(token))
    }

    fun receiptRead(token: Long, value: ByteArray, success: Boolean) {
        if (!matches(token, Phase.READING_RECEIPT)) return
        if (!success) { measured(false); fail("No se pudo verificar admisión del fragmento"); return }
        // Old reads are ignored; only the receipt for the current frame can advance this transfer.
        if (!value.contentEquals(MeshLinkProtocol.receipt(frames[index]))) {
            diagnostics.emit(MeshDiagnostic("receipt_ignored", token, frameIndex = index, status = "MISMATCH"))
            return
        }
        measured(true)
        index++
        if (index == frames.size) finish(MeshLinkOutcome.LINK_WRITTEN, "Escrito en el enlace; destino final sin confirmar")
        else writeFrame()
    }

    fun disconnected(token: Long) { if (job?.token == token) fail("Enlace mesh desconectado; destino sin confirmar") }
    fun cancel() { if (busy) finish(MeshLinkOutcome.CANCELLED, "Mesh cancelado; destino sin confirmar") }
    fun tick() { checkDeadline() }

    private fun checkDeadline(): Boolean {
        if (!busy) return false
        val now = clock.nowMillis()
        val expired = when {
            now - startedAt >= MeshBleTiming.JOB_MILLIS -> "JOB"
            now - operationAt >= MeshBleTiming.OPERATION_MILLIS -> "OPERATION"
            transferAt?.let { now - it >= transferLimit } == true -> "TRANSFER"
            else -> null
        }
        if (expired != null) {
            diagnostics.emit(MeshDiagnostic("gatt_timeout", job!!.token, phase.name,
                elapsedMillis = now - startedAt, frameIndex = index, frameCount = frames.size, status = expired))
            fail("Timeout mesh ($expired); destino sin confirmar")
        }
        return expired != null
    }

    private fun matches(token: Long, expected: Phase): Boolean {
        if (job?.token != token || phase != expected) return false
        return !checkDeadline()
    }
    private fun measured(success: Boolean) {
        diagnostics.emit(MeshDiagnostic("gatt_operation", job!!.token, phase.name,
            durationMillis = clock.nowMillis() - operationAt, elapsedMillis = clock.nowMillis() - startedAt,
            frameIndex = if (transferAt == null) null else index, frameCount = frames.size,
            transferId = job!!.transferId, status = if (success) "SUCCESS" else "FAILED"))
    }
    private fun writeFrame() = issue(Phase.WRITING_FRAME, MeshGattCommand.WriteFrame(job!!.token, frames[index].copyOf()))
    private fun issue(next: Phase, command: MeshGattCommand) {
        phase = next
        operationAt = clock.nowMillis()
        val accepted = try { port.execute(command) } catch (_: Exception) { false }
        if (!accepted) fail("No se pudo iniciar ${next.name}; destino sin confirmar")
    }
    private fun fail(detail: String) = finish(MeshLinkOutcome.FAILED, detail)
    private fun finish(outcome: MeshLinkOutcome, detail: String) {
        val previous = job ?: return
        val identity = remote
        diagnostics.emit(MeshDiagnostic("gatt_result", previous.token, phase.name,
            durationMillis = transferAt?.let { clock.nowMillis() - it }, elapsedMillis = clock.nowMillis() - startedAt,
            frameIndex = index, frameCount = frames.size, transferId = previous.transferId,
            messageId = previous.message?.messageId, ttl = previous.message?.hopsRemaining,
            peer = previous.address, nodeId = identity, status = outcome.name))
        job = null
        phase = Phase.IDLE
        frames = emptyList()
        remote = null
        try { port.execute(MeshGattCommand.Close(previous.token)) } catch (_: Exception) { }
        finished(MeshLinkResult(previous, outcome, identity, detail))
    }
}
