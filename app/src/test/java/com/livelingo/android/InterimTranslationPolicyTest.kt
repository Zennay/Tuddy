package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterimTranslationPolicyTest {
    @Test fun stableDelayAndBoundMatchArchivedIntentWithoutPerWordDownloading() {
        assertEquals(650L, InterimTranslationPolicy.STABLE_WAIT_MS)
        assertEquals(1500, InterimTranslationPolicy.MAX_INTERIM_CHARS)
        assertTrue(InterimTranslationPolicy.eligible("hallo wereld", "nl", "en", true))
    }

    @Test fun inactiveOrInvalidInterimCannotAllocateTranslator() {
        assertFalse(InterimTranslationPolicy.eligible("hallo", "nl", "en", false))
        assertFalse(InterimTranslationPolicy.eligible("", "nl", "en", true))
        assertFalse(InterimTranslationPolicy.eligible(" ", "nl", "en", true))
        assertFalse(InterimTranslationPolicy.eligible("a".repeat(1501), "nl", "en", true))
        assertTrue(InterimTranslationPolicy.eligible("a".repeat(1500), "nl", "en", true))
        assertFalse(InterimTranslationPolicy.eligible("test", "kmr", "en", true))
        assertFalse(InterimTranslationPolicy.eligible("test", "en", "ps", true))
    }

    @Test fun staleSpeechEpochNeverPublishesPriorTextOverNewInterim() {
        assertTrue(InterimTranslationPolicy.mayPublish(4, 4, 11, 11, true, true))
        assertFalse(InterimTranslationPolicy.mayPublish(4, 5, 11, 11, true, true))
        assertFalse(InterimTranslationPolicy.mayPublish(4, 4, 11, 12, true, true))
        assertFalse(InterimTranslationPolicy.mayPublish(4, 4, 11, 11, false, true))
        assertFalse(InterimTranslationPolicy.mayPublish(4, 4, 11, 11, true, false))
    }

    @Test fun sameTargetAndSourceMayRenderInterimTextLocally() {
        assertTrue(InterimTranslationPolicy.eligible("hello", "en", "en", true))
        assertTrue(InterimTranslationPolicy.mayPublish(2, 2, 0, 0, true, true))
    }
}
