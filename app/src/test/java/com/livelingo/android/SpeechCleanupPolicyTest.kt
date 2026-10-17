package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechCleanupPolicyTest {
    @Test fun releaseAlwaysCancelsBeforeDestroying() {
        val calls = mutableListOf<String>()
        SpeechCleanupPolicy.release(
            cancel = { calls.add("cancel") },
            destroy = { calls.add("destroy") }
        )
        assertEquals(listOf("cancel", "destroy"), calls)
    }

    @Test fun disconnectedCancelStillDestroysRecognizer() {
        val calls = mutableListOf<String>()
        SpeechCleanupPolicy.release(
            cancel = {
                calls.add("cancel")
                throw IllegalStateException("synthetic disconnected recognizer")
            },
            destroy = { calls.add("destroy") }
        )
        assertEquals(listOf("cancel", "destroy"), calls)
    }

    @Test fun destroyFailureDoesNotEscapeStopOrStartExceptionCleanup() {
        val calls = mutableListOf<String>()
        SpeechCleanupPolicy.release(
            cancel = { calls.add("cancel") },
            destroy = {
                calls.add("destroy")
                throw IllegalStateException("synthetic teardown rejection")
            }
        )
        assertEquals(listOf("cancel", "destroy"), calls)
    }

    @Test fun bothFailWithoutSkippingEitherOrResumingAnything() {
        val calls = mutableListOf<String>()
        SpeechCleanupPolicy.release(
            cancel = {
                calls.add("cancel")
                throw IllegalStateException("synthetic cancel failure")
            },
            destroy = {
                calls.add("destroy")
                throw IllegalStateException("synthetic destroy failure")
            }
        )
        assertEquals(listOf("cancel", "destroy"), calls)
    }

    @Test fun independentlyReusableAcrossMultipleSessions() {
        val calls = mutableListOf<String>()
        repeat(10) { index ->
            SpeechCleanupPolicy.release(
                cancel = { calls.add("cancel-$index") },
                destroy = { calls.add("destroy-$index") }
            )
        }
        assertEquals(20, calls.size)
        repeat(10) { index ->
            assertEquals("cancel-$index", calls[index * 2])
            assertEquals("destroy-$index", calls[index * 2 + 1])
        }
    }
}
