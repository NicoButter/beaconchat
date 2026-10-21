package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.core.MeshMessage
import com.nicobutter.beaconchat.mesh.core.NodeId
import com.nicobutter.beaconchat.mesh.transport.MeshFragmentProtocol
import java.nio.ByteBuffer
import java.util.UUID

/** Declared identity/capabilities, NOT cryptographic authentication. Fits the minimum ATT payload. */
object MeshLinkProtocol {
    const val HELLO_BYTES = 20
    fun hello(node: NodeId): ByteArray = ByteBuffer.allocate(HELLO_BYTES)
        .put(0xfa.toByte()).put(1).put(MeshMessage.VERSION.toByte()).put(MeshFragmentProtocol.VERSION.toByte())
        .putLong(node.value.mostSignificantBits).putLong(node.value.leastSignificantBits).array()

    fun identity(bytes: ByteArray): NodeId {
        require(bytes.size == HELLO_BYTES) { "Longitud de identidad mesh inválida" }
        val input = ByteBuffer.wrap(bytes)
        require(input.get() == 0xfa.toByte() && input.get() == 1.toByte() &&
            input.get() == MeshMessage.VERSION.toByte() && input.get() == MeshFragmentProtocol.VERSION.toByte()) {
            "Identidad/capacidades mesh incompatibles"
        }
        val id = UUID(input.long, input.long)
        require(id != UUID(0, 0)) { "Identidad mesh nula" }
        return NodeId(id)
    }

    /** Admission of this exact fragment only; never an end-to-end delivery ACK. */
    fun receipt(frame: ByteArray): ByteArray {
        MeshFragmentProtocol.parse(frame)
        return frame.copyOf(MeshFragmentProtocol.HEADER_BYTES)
    }
}

/** IDs remain unique even if Android retains a physical connection across connectGatt clients. */
internal class MeshTransferIds {
    private var next = 0
    private var wrapAfter = Long.MIN_VALUE
    fun allocate(now: Long): Int {
        check(next != 0 || now >= wrapAfter) { "IDs mesh agotados temporalmente" }
        val result = next
        next = (next + 1) and 0xffff
        if (next == 0) wrapAfter = if (now > Long.MAX_VALUE - MeshBleTiming.REASSEMBLY_MILLIS) Long.MAX_VALUE
            else now + MeshBleTiming.REASSEMBLY_MILLIS
        return result
    }
}
