package com.livelingo.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationWorkPolicyTest {
    @Test fun currentUnclearedConversationCanContinueOfflineTranslation() {
        assertTrue(TranslationWorkPolicy.mayContinue(2, 2, true))
        assertTrue(TranslationWorkPolicy.mayContinue(0, 0, true))
    }

    @Test fun clearingConversationPreventsDeferredModelDownloadAndTranslation() {
        assertFalse(TranslationWorkPolicy.mayContinue(2, 3, false))
        // Even if a caller accidentally retains the cleared line, a new
        // generation must block stale detection or model-download callbacks.
        assertFalse(TranslationWorkPolicy.mayContinue(2, 3, true))
    }

    @Test fun anEvictedOrMissingLineNeverStartsUnneededTranslation() {
        assertFalse(TranslationWorkPolicy.mayContinue(2, 2, false))
        assertFalse(TranslationWorkPolicy.mayContinue(3, 2, true))
    }
}
