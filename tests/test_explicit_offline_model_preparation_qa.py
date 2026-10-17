"""Android source-wired offline *text model* predownload acceptance.

Uses synthetic source policies only; does not pretend to validate a physical
handset's offline recognizer, speech packs, model storage or real MT accuracy.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/livelingo/android"


class OfflinePreparationSourceQA(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.activity = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        cls.policy = (SRC / "OfflineModelPreparationPolicy.kt").read_text(encoding="utf-8")
        cls.tests = (ROOT / "app/src/test/java/com/livelingo/android/"
                     "OfflineModelPreparationPolicyTest.kt").read_text(encoding="utf-8")

    def test_user_initiated_single_pair_ui_is_visible_and_bounded(self):
        src = self.activity
        self.assertIn("fun prepareOfflinePair()", src)
        self.assertIn("onClick = { prepareOfflinePair() }", src)
        self.assertIn('Text(if (preparingOfflineModel) "Tekstmodel voorbereiden…"', src)
        self.assertIn("if (preparingOfflineModel) return", src)
        self.assertIn("source.translateCode, target.translateCode, TargetCatalog.codes", src)
        self.assertIn("OfflinePairDecision.UNSUPPORTED", src)
        self.assertIn("OfflinePairDecision.ALREADY_SAME_LANGUAGE", src)
        self.assertNotIn("communityPhrasebooks", self.policy)

    def test_only_wifi_user_requested_model_download_and_no_cloud_speech(self):
        src = self.activity
        preamble = src.split("    fun prepareOfflinePair() {", 1)[1].split(
            "    // Queued work is valid only", 1)[0]
        self.assertIn("Translation.getClient(", preamble)
        self.assertIn("translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())",
                      preamble)
        self.assertEqual(1, preamble.count("downloadModelIfNeeded("))
        self.assertIn("translator.close()", preamble)
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(context)", src)
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer(context)", src)
        self.assertIn("RecognizerIntent.EXTRA_PREFER_OFFLINE, true", src)
        self.assertNotIn("DownloadConditions.Builder().build()", preamble)

    def test_stale_receipts_cannot_claim_model_ready_after_stop_or_language_change(self):
        src = self.activity
        preamble = src.split("    fun prepareOfflinePair() {", 1)[1].split(
            "    // Queued work is valid only", 1)[0]
        self.assertIn("val requestEpoch = offlinePreparationEpoch.begin()", preamble)
        self.assertEqual(3, preamble.count("offlinePreparationEpoch.isCurrent(requestEpoch)"))
        self.assertIn("offlinePreparationEpoch.invalidate()", src.split("    fun stop() {", 1)[1])
        self.assertIn("offlineModelStatus = \"\"", src)
        self.assertIn("epoch.invalidate()", self.tests)

    def test_on_failure_no_private_provider_exceptions_in_ui(self):
        src = self.activity
        preamble = src.split("    fun prepareOfflinePair() {", 1)[1].split(
            "    // Queued work is valid only", 1)[0]
        self.assertIn(".addOnFailureListener", preamble)
        self.assertIn("offlineModelStatus = \"Tekstmodel niet beschikbaar.", preamble)
        self.assertNotIn("localizedMessage", preamble)
        self.assertNotIn("exception.message", preamble)

    def test_jvm_covers_unsupported_community_and_rapid_old_receipts(self):
        for name in (
            "normalCrossLanguageTextPairIsEligibleOnlyOnExplicitRequest",
            "sameSourceAndTargetNeverTriggersWastefulDownload",
            "unsupportedCommunityPreviewsMustNotEnterMlKit",
            "allSupportedTargetsAreSupportedWithoutAutomaticPreloading",
            "changingLanguageOrStoppingInvalidatesAsyncCompletionReceipt",
            "fastRepeatedPreparationNeverAcceptsStaleOlderReceipt",
        ):
            self.assertIn(name, self.tests)
        self.assertIn("OfflinePairDecision.UNSUPPORTED", self.policy)
        self.assertIn("ALREADY_SAME_LANGUAGE", self.policy)


if __name__ == "__main__":
    unittest.main()
