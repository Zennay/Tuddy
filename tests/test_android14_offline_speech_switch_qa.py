"""Source-bound review of optional Android 14 *on-device* speech switching.

This check proves wiring and fail-closed policy, not actual language-pack
availability or a physical-device speech recognition result.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / "app/src/main/java/com/livelingo/android/MainActivity.kt").read_text(encoding="utf-8")
POLICY = (ROOT / "app/src/main/java/com/livelingo/android/"
          "OfflineSpeechLanguageSwitchPolicy.kt").read_text(encoding="utf-8")
JVM = (ROOT / "app/src/test/java/com/livelingo/android/"
       "OfflineSpeechLanguageSwitchPolicyTest.kt").read_text(encoding="utf-8")


class OfflineSpeechSourceSelectionQA(unittest.TestCase):
    def test_disabled_by_default_and_never_attempted_on_android12_13(self):
        self.assertIn("automaticSpeechSource by remember { mutableStateOf(false) }", MAIN)
        self.assertIn("Build.VERSION.SDK_INT >= 34 && offlineAvailable", MAIN)
        self.assertIn("apiLevel >= 34 && optedIn && onDeviceRecognizerAvailable", POLICY)
        self.assertIn("Switch(", MAIN)
        self.assertIn("enabled = speechSwitchSupported", MAIN)
        self.assertIn("stop()\n                            automaticSpeechSource = it", MAIN)

    def test_only_on_device_speech_receives_opt_in_detection_extras(self):
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer(context)", MAIN)
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(context)", MAIN)
        self.assertIn("RecognizerIntent.EXTRA_PREFER_OFFLINE, true", MAIN)
        self.assertIn("if (requestLanguageSwitch) {", MAIN)
        for extra in (
            "RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true",
            "RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,",
            "RecognizerIntent.LANGUAGE_SWITCH_BALANCED",
            "RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,",
            "ArrayList(allowedSpeechTags)",
        ):
            self.assertIn(extra, MAIN)
        self.assertIn("val allowedSpeechTags = spoken.map { it.speechCode }", MAIN)

    def test_async_callbacks_fenced_by_same_voice_generation(self):
        callback = MAIN.split("override fun onLanguageDetection(results: Bundle)", 1)[1]
        callback = callback.split("override fun onError(code: Int)", 1)[0]
        self.assertIn("!voiceSessions.isCurrent(voiceToken)", callback)
        self.assertIn("OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(", callback)
        self.assertIn("SpeechRecognizer.DETECTED_LANGUAGE", callback)
        self.assertIn("SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SKIPPED_NO_MODEL", callback)
        self.assertIn("SpeechRecognizer.LANGUAGE_SWITCH_RESULT_SUCCEEDED", callback)
        self.assertIn("OfflineSpeechLanguageSwitchPolicy.confirmedSwitch(", callback)
        self.assertLess(callback.index("confirmedSwitch("), callback.index("resolveDetectedTextSource("))
        self.assertIn("geïnstalleerde lokale spraakpakketten", callback)

    def test_only_valid_preselected_speech_tags_relabel_historical_caption(self):
        self.assertIn("availableSpokenTags.any", POLICY)
        self.assertIn("it.lowercase(Locale.ROOT) == tag", POLICY)
        self.assertIn("if (code in TargetCatalog.codes)", POLICY)
        self.assertIn('return selectedTextCode', POLICY)
        self.assertIn('Line(id, sentence, "Vertalen…", "Bezig", observedSource, selectedTarget)', MAIN)
        self.assertIn("translate(sentence, id, selectedTarget, observedSource, token)", MAIN)
        self.assertNotIn("cloud", POLICY.lower())

    def test_jvm_contract_proves_unsupported_and_api_gates(self):
        for case in (
            "onlyOptInAndroid14WithLocalRecognizerMayRequestSwitch",
            "recognizedAllowedTagsMapToMlKitTextSources",
            "unknownOrUnsupportedSpeechNeverBecomesClaimedTextLanguage",
            "validButUnselectedSpeechTagAlsoFallsBackToExplicitChoice",
            "emptyAllowedSetAlwaysKeepsExplicitChoice",
            "codeMustBeInReviewedMlKitCatalogDespiteSpokenTagWhitelisting",
        ):
            self.assertIn(case, JVM)
        self.assertIn("SpeechRecognizer.ERROR_NO_MATCH", MAIN)
        self.assertIn("stop() // never retry permissions", MAIN)


if __name__ == "__main__":
    unittest.main()
