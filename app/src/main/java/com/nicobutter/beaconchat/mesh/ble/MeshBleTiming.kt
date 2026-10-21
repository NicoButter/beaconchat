package com.nicobutter.beaconchat.mesh.ble

import com.nicobutter.beaconchat.mesh.transport.MeshFragmentProtocol

/** Local resource policy, not an RF throughput guarantee. Wire V1 remains unchanged. */
object MeshBleTiming {
    const val OPERATION_MILLIS = 5_000L
    const val LEGACY_TRANSFER_MILLIS = 14_000L
    // Experimental budget: 150 ms per WRITE or READ, plus 1 s scheduling allowance.
    const val FRAME_ROUND_TRIP_MILLIS = 300L
    const val SCHEDULING_MILLIS = 1_000L
    fun transferMillis(frames: Int): Long {
        require(frames in 1..MeshFragmentProtocol.MAX_FRAGMENTS)
        return maxOf(LEGACY_TRANSFER_MILLIS, frames * FRAME_ROUND_TRIP_MILLIS + SCHEDULING_MILLIS)
    }
    val MAX_TRANSFER_MILLIS = transferMillis(MeshFragmentProtocol.MAX_FRAGMENTS)
    val REASSEMBLY_MILLIS = MAX_TRANSFER_MILLIS + SCHEDULING_MILLIS
    // Connect, discover, read identity, write identity; none may consume 5 s itself.
    val JOB_MILLIS = 4 * OPERATION_MILLIS + MAX_TRANSFER_MILLIS
    val INCOMING_SESSION_MILLIS = JOB_MILLIS + SCHEDULING_MILLIS
    const val INCOMING_IDLE_MILLIS = 20_000L
    const val QUEUE_MILLIS = 30_000L
    const val NEIGHBOR_MILLIS = 30_000L
}
