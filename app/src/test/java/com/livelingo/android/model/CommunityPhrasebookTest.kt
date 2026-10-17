package com.livelingo.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only: the five curated entries are NOT general Kurdish/Pashto MT or ASR. */
class CommunityPhrasebookTest {
    @Test fun eachDialectIsIndependentAndScriptIsNotNormalizedAway() {
        assertEquals(setOf("kmr", "ckb", "ps"), CommunityPhrasebook.enabledTargets)
        assertEquals("kmr_Latn", CommunityPhrasebook.nllbTags["kmr"])
        assertEquals("ckb_Arab", CommunityPhrasebook.nllbTags["ckb"])
        assertEquals("pbt_Arab", CommunityPhrasebook.nllbTags["ps"])
        assertEquals("Silav", CommunityPhrasebook.translate("nl", "kmr", "hallo!").getOrNull())
        assertEquals("سڵاو", CommunityPhrasebook.translate("en", "ckb", "hello").getOrNull())
        assertEquals("سلام", CommunityPhrasebook.translate("nl", "ps", "hoi").getOrNull())
    }

    @Test fun allFiveCuratedConceptsWorkFromDutchAndEnglishWithoutNetwork() {
        val forms = listOf("hello" to "hallo", "thank you" to "bedankt",
            "yes" to "ja", "no" to "nee", "water" to "water")
        for (target in CommunityPhrasebook.enabledTargets) {
            for ((english, dutch) in forms) {
                val a = CommunityPhrasebook.translate("en", target, english).getOrThrow()
                val b = CommunityPhrasebook.translate("nl", target, dutch).getOrThrow()
                assertEquals("$target: $english", a, b)
                assertTrue(a.isNotBlank())
            }
        }
    }

    @Test fun hiddenControlsAndMultilineCannotForgeExactPhrasebookHits() {
        val forgedEnglish = listOf(
            "thank\\nyou".replace("\\n", "\n"),
            "thank\\tyou".replace("\\t", "\t"),
            "thank\u200Byou",
            "thank\u202Eyou",
            "thank\u2066you",
            "thank\uFEFFyou",
            "thank\u2028you",
            "thank\u2029you",
            "hello\u0000",
            "yes\r",
        )
        for (target in CommunityPhrasebook.enabledTargets) {
            for (phrase in forgedEnglish) {
                assertTrue("$target must reject hidden/multiline input",
                    CommunityPhrasebook.translate("en", target, phrase).isFailure)
            }
            assertEquals("Spas".takeIf { target == "kmr" },
                CommunityPhrasebook.translate("en", target, "THANK   YOU!").getOrNull()
                    .takeIf { target == "kmr" })
            assertTrue(CommunityPhrasebook.translate("nl", target, "dank\nje").isFailure)
        }
    }

    @Test fun oversizedAndMalformedTextCannotBecomeATranslation() {
        for (target in CommunityPhrasebook.enabledTargets) {
            for (source in listOf("nl", "en")) {
                assertTrue(CommunityPhrasebook.translate(source, target, " ".repeat(300) + "yes").isFailure)
                assertTrue(CommunityPhrasebook.translate(source, target, "\uD800yes").isFailure)
                assertTrue(CommunityPhrasebook.translate(source, target, "").isFailure)
            }
        }
    }

    @Test fun unknownSentencesUnsupportedSourcesAndNonPhrasebookTargetsFailClosed() {
        for (target in listOf("kmr", "ckb", "ps")) {
            assertTrue(CommunityPhrasebook.translate("en", target, "I need urgent help").isFailure)
            assertTrue(CommunityPhrasebook.translate("nl", target, "medicine").isFailure)
            assertTrue(CommunityPhrasebook.translate("fa", target, "yes").isFailure)
            assertTrue(CommunityPhrasebook.translate(null, target, "hello").isFailure)
        }
        for (target in listOf("ku", "de", "", "KMR")) {
            assertTrue(CommunityPhrasebook.translate("en", target, "yes").isFailure)
        }
        assertFalse(CommunityPhrasebook.translate("en", "kmr", "  THANKS!  ").isFailure)
    }
}
