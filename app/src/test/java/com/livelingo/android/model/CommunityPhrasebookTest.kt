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
