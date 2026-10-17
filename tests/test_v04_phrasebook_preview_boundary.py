"""Source-level guard: bundled Kurdish/Pashto preview is never a cloud/ML Kit target.

Android JVM tests validate outputs; these checks preserve the deliberately
separate UI path before hosted Gradle or emulator work starts.
"""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"


class CommunityPreviewBoundary(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        cls.phrases = (APP / "model/CommunityPhrasebook.kt").read_text(encoding="utf-8")
        cls.targets = (APP / "TargetCatalog.kt").read_text(encoding="utf-8")

    def test_preview_cannot_become_mlkit_target_or_online_fallback(self):
        self.assertIn("CommunityPhrasebook.translate(previewSource, previewTarget, previewInput)", self.ui)
        self.assertIn("val selectedTarget = target.translateCode", self.ui)
        self.assertIn("val selectedSource = source.translateCode", self.ui)
        self.assertIn("TranslateLanguage.fromLanguageTag", self.ui)
        self.assertIn("val enabledTargets = setOf(\"kmr\", \"ckb\", \"ps\")", self.phrases)
        self.assertNotRegex(self.targets, r'"(?:kmr|ckb|ps)"')
        for network in ("http://", "https://", "SpeechRecognizer", "TranslatorOptions", "DownloadConditions", "URLConnection"):
            self.assertNotIn(network, self.phrases)

    def test_exactly_five_bundled_phrase_entries_and_no_fallback(self):
        self.assertEqual(len(re.findall(r"^\s*Entry\(listOf\(", self.phrases, flags=re.M)), 5)
        self.assertIn("No translation invented.", self.phrases)
        self.assertIn("Result.failure", self.phrases)
        self.assertIn("source !in setOf(\"en\", \"nl\")", self.phrases)

    def test_manual_preview_is_bounded_and_erased_with_conversation(self):
        self.assertIn("previewInput = it.take(80)", self.ui)
        self.assertIn("previewInput = \"\"", self.ui)
        self.assertIn("previewResult = null", self.ui)
        self.assertIn("Geen spraakherkenning".lower(), self.ui.lower())
        self.assertIn("Niet geschikt voor medisch", self.ui)
        self.assertIn("CommunityPhrasebook.translate", self.ui)


if __name__ == "__main__":
    unittest.main()
