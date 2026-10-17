package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LiveFollowPolicyTest {
    @Test fun noCaptionsMeansNoScroll() {
        assertNull(LiveFollowPolicy.targetIndex(0, false, true))
        assertNull(LiveFollowPolicy.targetIndex(0, false, false))
    }

    @Test fun firstProvisionalCaptionIsVisibleEvenWithZeroFinalLines() {
        assertEquals(0, LiveFollowPolicy.targetIndex(0, true, true))
        assertEquals(1, LiveFollowPolicy.targetIndex(1, true, true))
        assertEquals(500, LiveFollowPolicy.targetIndex(500, true, true))
    }

    @Test fun finalCaptionUsesLastExistingZeroBasedIndex() {
        assertEquals(0, LiveFollowPolicy.targetIndex(1, false, true))
        assertEquals(499, LiveFollowPolicy.targetIndex(500, false, true))
    }

    @Test fun turningFollowOffPreventsAnyAutomaticScroll() {
        for (count in listOf(0, 1, 500)) {
            assertNull(LiveFollowPolicy.targetIndex(count, true, false))
            assertNull(LiveFollowPolicy.targetIndex(count, false, false))
        }
    }

    @Test fun invalidNegativeCountMustFailBeforeIndexingLazyList() {
        assertThrows(IllegalArgumentException::class.java) {
            LiveFollowPolicy.targetIndex(-1, true, true)
        }
    }
}
