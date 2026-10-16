package com.nicobutter.beaconchat.mesh

/** Guards duplicate/late callbacks; errors, cancellation and timeout share terminal handling. */
internal class DirectSendState {
    enum class Phase { CONNECTING, DISCOVERING, WRITING, FINISHED }
    var phase = Phase.CONNECTING
        private set

    fun connected(): Boolean = advance(Phase.CONNECTING, Phase.DISCOVERING)
    fun servicesDiscovered(): Boolean = advance(Phase.DISCOVERING, Phase.WRITING)
    fun finish(): Boolean {
        if (phase == Phase.FINISHED) return false
        phase = Phase.FINISHED
        return true
    }
    private fun advance(expected: Phase, next: Phase): Boolean {
        if (phase != expected) return false
        phase = next
        return true
    }
}
