package com.livelingo.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalGlossaryTest {
    @Test fun curatedLanguageSpecificEntriesAreAvailableOffline() {
        assertEquals("💧", LocalGlossary.lookup("de-DE", "WASSER")?.illustration)
        assertEquals("💧", LocalGlossary.lookup("en-US", "water")?.illustration)
        assertEquals("👋", LocalGlossary.lookup("fr-FR", "bonjour")?.illustration)
        assertEquals("🏠", LocalGlossary.lookup("es-ES", "CASA")?.illustration)
        assertEquals("🐈", LocalGlossary.lookup("nl-NL", "kat")?.illustration)
        assertEquals("💡", LocalGlossary.lookup("de", "Verstehst")?.illustration)
    }

    @Test fun untranslatedUnsupportedAndSensitiveTermsNeverGetInventedMeanings() {
        assertNull(LocalGlossary.lookup("en", "quasar"))
        assertNull(LocalGlossary.lookup("ps", "medicine"))
        assertNull(LocalGlossary.lookup("ckb", "help"))
        assertNull(LocalGlossary.lookup("kmr", "water"))
        assertNull(LocalGlossary.lookup("so", "water"))
        assertNull(LocalGlossary.lookup(null, "water"))
        assertNull(LocalGlossary.lookup("en", "medical-legal-advice"))
        assertNull(LocalGlossary.lookup("en", ""))
    }

    @Test fun bcp47RegionAndCapitalizationAreNormalizedWithoutNetwork() {
        assertEquals(LocalGlossary.lookup("nl", "water"), LocalGlossary.lookup("NL_nl", " WATER "))
        assertEquals(LocalGlossary.lookup("en", "hello"), LocalGlossary.lookup("en-US", "'HELLO'"))
        assertNull(LocalGlossary.lookup("en-US", "hello there"))
        assertEquals("A greeting used when meeting someone.", LocalGlossary.lookup("en", "hello")?.definition)
    }
}
