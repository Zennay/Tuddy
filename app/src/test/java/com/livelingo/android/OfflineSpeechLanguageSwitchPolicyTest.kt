package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSpeechLanguageSwitchPolicyTest {
    private val tags = listOf("nl-NL", "en-US", "de-DE", "ar-SA", "es-ES", "fr-FR")

    @Test fun onlyOptInAndroid14WithLocalRecognizerMayRequestSwitch() {
        assertFalse(OfflineSpeechLanguageSwitchPolicy.shouldRequest(31, true, true))
        assertFalse(OfflineSpeechLanguageSwitchPolicy.shouldRequest(33, true, true))
        assertFalse(OfflineSpeechLanguageSwitchPolicy.shouldRequest(34, false, true))
        assertFalse(OfflineSpeechLanguageSwitchPolicy.shouldRequest(34, true, false))
        assertTrue(OfflineSpeechLanguageSwitchPolicy.shouldRequest(34, true, true))
        assertTrue(OfflineSpeechLanguageSwitchPolicy.shouldRequest(35, true, true))
    }

    @Test fun onlyAndroidConfirmedSwitchMayChangeCaptionLanguage() {
        assertTrue(OfflineSpeechLanguageSwitchPolicy.confirmedSwitch(1, 1))
        for (unconfirmed in listOf(-1, 0, 2, 3, Int.MAX_VALUE)) {
            assertFalse(OfflineSpeechLanguageSwitchPolicy.confirmedSwitch(unconfirmed, 1))
        }
    }

    @Test fun successfulLanguageDetectionIsNotAConfirmedSwitch() {
        // A valid detected locale can be returned while Android reports
        // NOT_ATTEMPTED/FAILED/SKIPPED_NO_MODEL; the callback must gate this.
        assertEquals("en", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "en-US", "nl", tags))
        for (unconfirmed in listOf(0, 2, 3)) {
            assertFalse(OfflineSpeechLanguageSwitchPolicy.confirmedSwitch(unconfirmed, 1))
        }
    }

    @Test fun recognizedAllowedTagsMapToMlKitTextSources() {
        assertEquals("en", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource("en-US", "nl", tags))
        assertEquals("de", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource("de-DE", "nl", tags))
        assertEquals("ar", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource("ar-SA", "nl", tags))
        assertEquals("fr", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource("FR-fr", "nl",
            listOf("fr-FR")))
        assertEquals("en", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource("en_US", "nl", tags))
    }

    @Test fun unknownOrUnsupportedSpeechNeverBecomesClaimedTextLanguage() {
        for (guess in listOf<String?>(null, "", "xx-XX", "ckb", "kmr", "ps", "so-SO",
            "https://bad/", "en-US\nmalformed", " ar-SA@evil ")) {
            assertEquals(guess ?: "null", "nl",
                OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(guess, "nl", tags))
        }
    }

    @Test fun validButUnselectedSpeechTagAlsoFallsBackToExplicitChoice() {
        assertEquals("nl", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "pt-PT", "nl", tags))
        assertEquals("nl", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "ar-PS", "nl", tags))
        assertEquals("nl", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "en", "nl", tags))
    }

    @Test fun emptyAllowedSetAlwaysKeepsExplicitChoice() {
        assertEquals("de", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "en-US", "de", emptyList()))
    }

    @Test fun codeMustBeInReviewedMlKitCatalogDespiteSpokenTagWhitelisting() {
        assertEquals("nl", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "ps-AF", "nl", listOf("ps-AF")))
        assertEquals("nl", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "ckb-IQ", "nl", listOf("ckb-IQ")))
        assertEquals("en", OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(
            "en-US", "nl", listOf("en-US")))
    }
}
