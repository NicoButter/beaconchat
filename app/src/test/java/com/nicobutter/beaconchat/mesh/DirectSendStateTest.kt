package com.nicobutter.beaconchat.mesh

import org.junit.Assert.*
import org.junit.Test

class DirectSendStateTest {
    @Test fun normalSequenceAndDuplicateCallbacks() {
        val state = DirectSendState()
        assertFalse(state.servicesDiscovered())
        assertTrue(state.connected())
        assertFalse(state.connected())
        assertTrue(state.servicesDiscovered())
        assertFalse(state.servicesDiscovered())
        assertEquals(DirectSendState.Phase.WRITING, state.phase)
        assertTrue(state.finish())
        assertFalse(state.finish())
    }
    @Test fun errorCancellationOrTimeoutIsTerminalAtEveryStage() {
        for (stage in 0..2) {
            val state = DirectSendState()
            if (stage >= 1) state.connected()
            if (stage >= 2) state.servicesDiscovered()
            assertTrue(state.finish())
            assertEquals(DirectSendState.Phase.FINISHED, state.phase)
            assertFalse(state.connected())
            assertFalse(state.servicesDiscovered())
            assertFalse(state.finish())
        }
    }
    @Test fun newAttemptCanProceedAfterFailure() {
        val failed = DirectSendState()
        failed.finish()
        val retry = DirectSendState()
        assertTrue(retry.connected())
        assertFalse(failed.connected())
        assertTrue(retry.servicesDiscovered())
    }
}
