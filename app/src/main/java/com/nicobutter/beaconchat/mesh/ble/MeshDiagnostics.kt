package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.MessageId
import com.nicobutter.beaconchat.mesh.core.NodeId
import com.nicobutter.beaconchat.mesh.transport.MeshLinkSession

/** Metadata only. event/status must be fixed labels, never content, exceptions or credentials. */
data class MeshDiagnostic(val event: String, val token: Long? = null, val phase: String? = null,
    val durationMillis: Long? = null, val elapsedMillis: Long? = null,
    val frameIndex: Int? = null, val frameCount: Int? = null, val transferId: Int? = null,
    val messageId: MessageId? = null, val ttl: Int? = null, val peer: String? = null,
    val nodeId: NodeId? = null, val session: MeshLinkSession? = null, val status: String? = null)

fun interface MeshDiagnostics {
    fun record(event: MeshDiagnostic)
    companion object { val NONE = MeshDiagnostics { } }
}

/** Diagnostics must not interrupt admission, receipts or connection cleanup. */
internal fun MeshDiagnostics.emit(event: MeshDiagnostic) { try { record(event) } catch (_: Exception) { } }
