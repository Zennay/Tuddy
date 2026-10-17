package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionRetentionPolicyTest {
    @Test fun underLimitDoesNotRemoveAnything() {
        for (size in listOf(0, 1, 499, 500)) {
            assertEquals(0, CaptionRetentionPolicy.oldestToDrop(size))
        }
    }

    @Test fun oldestEntriesAreDroppedExactlyAtBoundary() {
        assertEquals(1, CaptionRetentionPolicy.oldestToDrop(501))
        assertEquals(2, CaptionRetentionPolicy.oldestToDrop(502))
        assertEquals(500, CaptionRetentionPolicy.oldestToDrop(1000))
        assertEquals(49_500, CaptionRetentionPolicy.oldestToDrop(50_000))
    }

    @Test fun repeatedStreamRetainsOnlyLatestFiveHundredCaptions() {
        val captions = mutableListOf<Int>()
        repeat(5_000) { id ->
            captions.add(id)
            repeat(CaptionRetentionPolicy.oldestToDrop(captions.size)) {
                captions.removeAt(0)
            }
            assertTrue(captions.size <= CaptionRetentionPolicy.MAX_CAPTIONS)
        }
        assertEquals(500, captions.size)
        assertEquals(4_500, captions.first())
        assertEquals(4_999, captions.last())
        assertFalse(captions.contains(4_499))
    }

    @Test fun unsafeNegativeCountIsNeverTreatedAsAValidSnapshot() {
        assertThrows(IllegalArgumentException::class.java) {
            CaptionRetentionPolicy.oldestToDrop(-1)
        }
    }

    @Test fun retiredLineCannotBeRepopulatedByAnOldTranslationCallback() {
        assertFalse(TranslationWorkPolicy.mayContinue(5, 5, false))
        assertTrue(TranslationWorkPolicy.mayContinue(5, 5, true))
        assertFalse(TranslationWorkPolicy.mayContinue(5, 6, true))
    }
}
