package com.nicobutter.beaconchat.mesh.core

import java.util.UUID

/** Logical identities, never Bluetooth addresses or display names. Ownership is supplied by the caller. */
data class NodeId(val value: UUID)
data class MessageId(val value: UUID)
data class MessageKey(val originId: NodeId, val messageId: MessageId)

/**
 * Immutable logical envelope, not a BLE frame. null destination means broadcast.
 * hopsRemaining counts further link transmissions: a receiver at zero may deliver, never forward.
 * Forwarding preserves every field except hopsRemaining; retries must preserve the same key/content.
 */
data class MeshMessage(
    val messageId: MessageId,
    val originId: NodeId,
    val destinationId: NodeId?,
    val content: String,
    val hopsRemaining: Int,
    val version: Int = VERSION
) {
    val key: MessageKey get() = MessageKey(originId, messageId)

    companion object { const val VERSION = 1 }
}

/** Local monotonic time only; nodes do not need synchronized wall clocks. */
fun interface MeshClock { fun nowMillis(): Long }

/** A synchronous, nonblocking one-hop handoff. ACCEPTED is not a recipient/delivery ACK. */
fun interface MeshTransport {
    fun forward(message: MeshMessage, excludedNodes: Set<NodeId>): Handoff
}

enum class Handoff { ACCEPTED, UNAVAILABLE }
