package com.livelingo.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageAndWordUnitTest {
    @Test fun autoLanguageLabelsAreNormalizedSafely() {
        assertEquals("de", Languages.normalize("de-DE"))
        assertEquals("nl", Languages.normalize("NL_nl"))
        assertEquals("zh", Languages.normalize("zh-Hans"))
    }

    @Test fun largeCatalogAndHumanitarianLanguagesAreExposedWithoutFakeOfflineSupport() {
        assertEquals(59, Languages.choices.size)
        assertEquals(59, Languages.choices.map { it.code }.toSet().size)
        assertEquals("ar", Languages.normalize("ar-PS"))
        assertEquals("fa", Languages.normalize("fa-IR"))
        assertEquals("ar", Languages.search("عرب").first().code)
        assertEquals("fa", Languages.search("persian").first().code)
        assertNull(Languages.find("so"))
        assertEquals(true, Languages.requested.any { it.code == "so" })
        assertEquals(true, Languages.isCommunityPhrasebook("kmr"))
        assertEquals("ps", Languages.searchCommunity("Pasjtoe").single().code)
        assertEquals("Silav", CommunityPhrasebook.translate("nl", "kmr", "Hallo!").getOrNull())
        assertEquals("سڵاو", CommunityPhrasebook.translate("en", "ckb", "Hello").getOrNull())
        assertEquals("مننه", CommunityPhrasebook.translate("nl", "ps", "Dank je").getOrNull())
        assertEquals(true, CommunityPhrasebook.translate("en", "ps", "I need medicine").isFailure)
        assertEquals(listOf("de", "ar", "en"), LanguagePairPlan.starterSources("nl", "de"))
    }

    @Test fun pressSelectsSingleWordIncludingNonAscii() {
        assertEquals("Verstehst", wordAt("Verstehst du?", 4))
        assertEquals("l’amour", wordAt("l’amour", 3))
        assertNull(wordAt("hi!", 2))
    }

    @Test fun glossaryNeverInventsMeanings() {
        assertEquals("💧", LocalGlossary.lookup("de-DE", "WASSER")?.illustration)
        assertNull(LocalGlossary.lookup("en", "quasar"))
    }

    @Test fun historicalLinesKeepTheirOriginalTargetAfterSelectingAnotherOne() {
        val session = Conversation()
            .finish("Bonjour", 100, "fr", "nl")
            .finish("Guten Morgen", 200, "de", "en")
        assertEquals(listOf("nl", "en"), session.lines.map { it.targetLanguage })
        assertEquals(listOf("fr", "de"), session.lines.map { it.sourceLanguage })
    }

    @Test fun aDelayedResultCannotAttachToTheFirstLineOfANewSession() {
        val finished = Conversation().finish("first", 100)
        val cleared = finished.clear()
        val newCaption = cleared.finish("second", 200)
        assertEquals(2L, newCaption.lines.single().id)
        assertNull(newCaption.finalTranslation(1L, "wrong").lines.single().translation)
    }
}
