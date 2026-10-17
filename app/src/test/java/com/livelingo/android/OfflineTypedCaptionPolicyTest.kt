package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineTypedCaptionPolicyTest {
    @Test fun textOnlyMessagePreservesUnicodeAndNormalizesWhitespace() {
        assertEquals("مرحبا 👋", OfflineTypedCaptionPolicy.normalize(" \nمرحبا 👋\n"))
        assertEquals("Hallo\nwereld", OfflineTypedCaptionPolicy.normalize("\tHallo\nwereld\t"))
    }

    @Test fun emptyOrWhitespaceOnlyMessageNeverCreatesCaption() {
        assertNull(OfflineTypedCaptionPolicy.normalize(""))
        assertNull(OfflineTypedCaptionPolicy.normalize(" \t \n "))
    }

    @Test fun providerAndHugeInputAreBoundedWithoutTruncatingSurrogatePair() {
        assertEquals("a".repeat(1500), OfflineTypedCaptionPolicy.normalize("a".repeat(1500)))
        assertNull(OfflineTypedCaptionPolicy.normalize("a".repeat(1501)))
        assertNull(OfflineTypedCaptionPolicy.normalize("🌍".repeat(751)))
        assertEquals("🌍".repeat(750), OfflineTypedCaptionPolicy.normalize("🌍".repeat(750)))
    }

    @Test fun binaryAndInvisibleControlsAreRejectedRatherThanRendered() {
        for (bad in listOf("a\u0000b", "a\u0007b", "a\r" + "b", "a\u007fb")) {
            assertNull(OfflineTypedCaptionPolicy.normalize(bad))
        }
    }

    @Test fun ordinaryNewlineAndTabInputRemainsReadable() {
        assertEquals("regel 1\nregel 2", OfflineTypedCaptionPolicy.normalize(
            " regel 1\nregel 2 "))
    }
}
