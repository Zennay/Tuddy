package com.livelingo.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineModelPreparationPolicyTest {
    private val codes = TargetCatalog.codes

    @Test fun normalCrossLanguageTextPairIsEligibleOnlyOnExplicitRequest() {
        assertEquals(OfflinePairDecision.MODEL_REQUIRED,
            OfflineModelPreparationPolicy.decide("nl", "en", codes))
        assertEquals(OfflinePairDecision.MODEL_REQUIRED,
            OfflineModelPreparationPolicy.decide("ar", "nl", codes))
    }

    @Test fun sameSourceAndTargetNeverTriggersWastefulDownload() {
        assertEquals(OfflinePairDecision.ALREADY_SAME_LANGUAGE,
            OfflineModelPreparationPolicy.decide("de", "de", codes))
    }

    @Test fun unsupportedCommunityPreviewsMustNotEnterMlKit() {
        for (code in listOf("kmr", "ckb", "ps", "so", "ti", "rhg", "")) {
            assertEquals(code, OfflinePairDecision.UNSUPPORTED,
                OfflineModelPreparationPolicy.decide(code, "nl", codes))
            assertEquals(code, OfflinePairDecision.UNSUPPORTED,
                OfflineModelPreparationPolicy.decide("nl", code, codes))
        }
    }

    @Test fun allSupportedTargetsAreSupportedWithoutAutomaticPreloading() {
        assertEquals(59, codes.size)
        for (code in codes) {
            assertEquals(code,
                if (code == "nl") OfflinePairDecision.ALREADY_SAME_LANGUAGE
                else OfflinePairDecision.MODEL_REQUIRED,
                OfflineModelPreparationPolicy.decide("nl", code, codes))
        }
    }

    @Test fun changingLanguageOrStoppingInvalidatesAsyncCompletionReceipt() {
        val epoch = OfflinePreparationEpoch()
        val first = epoch.begin()
        assertTrue(epoch.isCurrent(first))
        val newer = epoch.begin()
        assertFalse(epoch.isCurrent(first))
        assertTrue(epoch.isCurrent(newer))
        epoch.invalidate() // Stop / Activity ON_STOP
        assertFalse(epoch.isCurrent(newer))
        assertFalse(epoch.isCurrent(0))
    }

    @Test fun fastRepeatedPreparationNeverAcceptsStaleOlderReceipt() {
        val epoch = OfflinePreparationEpoch()
        val first = epoch.begin()
        repeat(200) {
            val next = epoch.begin()
            assertFalse(epoch.isCurrent(first))
            assertTrue(epoch.isCurrent(next))
        }
    }
}
