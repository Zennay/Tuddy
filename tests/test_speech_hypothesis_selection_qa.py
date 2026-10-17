"""Source-wiring checks for bounded Android recognition hypothesis selection.

No microphone, phone, cloud request or decoded personal transcript is used.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"


class BoundedHypothesisSourceQA(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.activity = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        cls.policy = (APP / "SpeechHypothesisPolicy.kt").read_text(encoding="utf-8")
        cls.jvm = (ROOT / "app/src/test/java/com/livelingo/android/"
                   "SpeechHypothesisPolicyTest.kt").read_text(encoding="utf-8")

    def test_final_and_interim_use_same_policy(self):
        self.assertEqual(2, self.activity.count("SpeechHypothesisPolicy.select("))
        self.assertIn("val sentence = SpeechHypothesisPolicy.select(", self.activity)
        self.assertIn("partial = SpeechHypothesisPolicy.select(", self.activity)
        self.assertNotIn("?.firstOrNull()?.trim().orEmpty()", self.activity)
        self.assertNotIn("?.firstOrNull().orEmpty()", self.activity)

    def test_no_on_device_fallback_regression(self):
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer(context)", self.activity)
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(context)", self.activity)
        self.assertIn("RecognizerIntent.EXTRA_PREFER_OFFLINE, true", self.activity)
        self.assertIn("if (!voiceSessions.isCurrent(voiceToken)) return", self.activity)

    def test_bounded_and_fail_closed_provider_response(self):
        self.assertIn("MAX_ALTERNATIVES = 8", self.policy)
        self.assertIn("MAX_CAPTION_CHARS = 1500", self.policy)
        self.assertIn("candidates.take(MAX_ALTERNATIVES)", self.policy)
        self.assertIn("text.isNotBlank() && text.length <= MAX_CAPTION_CHARS", self.policy)
        self.assertIn('return ""', self.policy)
        for forbidden in ("http://", "https://", "FileOutputStream", "SharedPreferences",
                          "Translation.getClient(", "RecognizerIntent", "SpeechRecognizer.create"):
            self.assertNotIn(forbidden, self.policy)

    def test_regression_covers_real_blank_top_and_unicode(self):
        for method in ("usesSecondHypothesisWhenFirstIsBlank",
                       "skipsNullAndWhitespaceButPreservesArabicAndEmoji",
                       "firstGoodCandidateAlwaysWinsWithoutConcatenatingAlternatives",
                       "nullAndEntirelyBlankResultsNeverInventCaptions",
                       "oversizedHypothesisSkippedWithoutTruncatingUnicode",
                       "ignoresAlternativesBeyondEightForBoundedProviderInput"):
            self.assertIn(method, self.jvm)
        self.assertIn("مرحبا", self.jvm)


if __name__ == "__main__":
    unittest.main()
