package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechHypothesisPolicyTest {
    @Test fun usesSecondHypothesisWhenFirstIsBlank() {
        assertEquals("Hallo wereld", SpeechHypothesisPolicy.select(
            listOf("", "  Hallo wereld  ", "irrelevant")))
    }

    @Test fun skipsNullAndWhitespaceButPreservesArabicAndEmoji() {
        assertEquals("مرحبا 👋", SpeechHypothesisPolicy.select(
            listOf(null, "   ", "\n\t", " مرحبا 👋 ")))
    }

    @Test fun firstGoodCandidateAlwaysWinsWithoutConcatenatingAlternatives() {
        assertEquals("best", SpeechHypothesisPolicy.select(listOf("best", "second")))
    }

    @Test fun nullAndEntirelyBlankResultsNeverInventCaptions() {
        assertEquals("", SpeechHypothesisPolicy.select(null))
        assertEquals("", SpeechHypothesisPolicy.select(emptyList()))
        assertEquals("", SpeechHypothesisPolicy.select(listOf("", "  ", null, "\n")))
    }

    @Test fun oversizedHypothesisSkippedWithoutTruncatingUnicode() {
        assertEquals("valid", SpeechHypothesisPolicy.select(
            listOf("a".repeat(1501), "valid")))
        assertEquals("é".repeat(1500), SpeechHypothesisPolicy.select(
            listOf("é".repeat(1500))))
        assertEquals("", SpeechHypothesisPolicy.select(
            listOf("é".repeat(1501))))
    }

    @Test fun ignoresAlternativesBeyondEightForBoundedProviderInput() {
        assertEquals("", SpeechHypothesisPolicy.select(
            List<String?>(8) { " " } + listOf("nine must be ignored")))
        assertEquals("eighth", SpeechHypothesisPolicy.select(
            List<String?>(7) { "" } + listOf("eighth", "ninth")))
    }
}
