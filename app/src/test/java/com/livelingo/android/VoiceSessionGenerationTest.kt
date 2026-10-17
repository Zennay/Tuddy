package com.livelingo.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSessionGenerationTest {
    @Test fun androidEndOfSpeechAndRapidRestartRejectOldFinalAndInterimResults() {
        val gate = VoiceSessionGeneration()
        val first = gate.begin()
        assertTrue(gate.isCurrent(first))
        // Android can end recognition without the user hitting Stop.
        // Starting another recognition must itself invalidate old callbacks.
        val next = gate.begin()
        assertNotEquals(first, next)
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(next))
    }

    @Test fun stopOrBackgroundInvalidatesOutstandingSpeechCallbacks() {
        val gate = VoiceSessionGeneration()
        val first = gate.begin()
        gate.invalidate()
        assertFalse(gate.isCurrent(first))
        assertFalse(gate.isCurrent(0))
        val restarted = gate.begin()
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(restarted))
    }

    @Test fun successiveSessionsNeverReuseFirstGeneration() {
        val gate = VoiceSessionGeneration()
        val first = gate.begin()
        repeat(200) {
            val next = gate.begin()
            assertFalse(gate.isCurrent(first))
            assertTrue(gate.isCurrent(next))
        }
    }
}
