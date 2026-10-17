package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContinuousSpeechRestartPolicyTest {
    @Test fun successfulSentenceRestartsAfterBoundedDelay() {
        assertEquals(350L, ContinuousSpeechRestartPolicy.RESULT_DELAY_MS)
    }

    @Test fun onlyNoMatchAndSpeechTimeoutMayRetry() {
        // Synthetic distinct Android recognition error codes; policy is pure JVM.
        val noMatch = 7
        val timeout = 6
        assertEquals(850L, ContinuousSpeechRestartPolicy.delayForError(noMatch, noMatch, timeout))
        assertEquals(850L, ContinuousSpeechRestartPolicy.delayForError(timeout, noMatch, timeout))
        for (code in listOf(-1, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12)) {
            assertNull("Unexpected error $code should be fatal", ContinuousSpeechRestartPolicy.delayForError(code, noMatch, timeout))
        }
    }

    @Test fun invalidErrorCodeMappingFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            ContinuousSpeechRestartPolicy.delayForError(7, 7, 7)
        }
    }
}
