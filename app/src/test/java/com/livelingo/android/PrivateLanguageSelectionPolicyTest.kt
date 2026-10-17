package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Test

class PrivateLanguageSelectionPolicyTest {
    private val spoken = listOf("nl-NL", "en-US", "de-DE", "fr-FR", "es-ES", "ar-SA")

    @Test fun historicalFrenchTargetRestoresFromCanonicalCatalog() {
        assertEquals("fr", PrivateLanguageSelectionPolicy.restoreTarget(
            "fr", TargetCatalog.codes, "en"))
        assertEquals("ar-SA", PrivateLanguageSelectionPolicy.restoreSource(
            "ar-SA", spoken, "nl-NL"))
    }

    @Test fun missingAndCorruptPreferencesNeverBecomePickerValues() {
        for (bad in listOf<String?>(null, "", "en-US\n", "ps", "ckb", "kmr", "http://x", "../en")) {
            assertEquals("en", PrivateLanguageSelectionPolicy.restoreTarget(
                bad, TargetCatalog.codes, "en"))
            assertEquals("nl-NL", PrivateLanguageSelectionPolicy.restoreSource(
                bad, spoken, "nl-NL"))
        }
    }

    @Test fun textCatalogDoesNotMasqueradeAsSpeechPacks() {
        assertEquals("nl-NL", PrivateLanguageSelectionPolicy.restoreSource(
            "zh", spoken, "nl-NL"))
        assertEquals("zh", PrivateLanguageSelectionPolicy.restoreTarget(
            "zh", TargetCatalog.codes, "en"))
        assertEquals("nl-NL", PrivateLanguageSelectionPolicy.restoreSource(
            "en", spoken, "nl-NL"))
    }

    @Test fun exactCodesStayCaseSensitiveToAvoidAmbiguousRestoration() {
        assertEquals("en", PrivateLanguageSelectionPolicy.restoreTarget(
            "FR", TargetCatalog.codes, "en"))
        assertEquals("nl-NL", PrivateLanguageSelectionPolicy.restoreSource(
            "EN-US", spoken, "nl-NL"))
    }

    @Test fun noTranscriptsOrSecretsExistInOnDiskKeyContract() {
        assertEquals("tuddy-language-selection", PrivateLanguageSelectionPolicy.SETTINGS_NAME)
        assertEquals("spoken-source-tag", PrivateLanguageSelectionPolicy.SOURCE_KEY)
        assertEquals("translation-target-code", PrivateLanguageSelectionPolicy.TARGET_KEY)
    }
}
