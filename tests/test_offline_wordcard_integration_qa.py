"""Offline, accessible word-card integration preserves historical language identity.

Only human-tapped or explicitly entered words are shown. The dictionary must
abstain on unsupported words/languages instead of making network requests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"
UI = APP / "MainActivity.kt"
WORD = APP / "ui/OfflineWordText.kt"

class OfflineWordCardIntegrationQA(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = UI.read_text(encoding="utf-8")
        cls.word = WORD.read_text(encoding="utf-8")

    def test_original_and_translation_preserve_per_caption_language(self):
        for marker in ("val sourceCode: String", "val targetCode: String",
                       'Line(id, sentence, "Vertalen…", "Bezig", observedSource, selectedTarget)',
                       "lines[row].copy(sourceCode = from)",
                       "OfflineWordText(", "line.original, line.sourceCode",
                       "line.translated, line.targetCode"):
            self.assertIn(marker, self.ui)
        self.assertIn('if (line.status == "Lokaal vertaald" || line.status == "Lokaal")', self.ui)
        self.assertIn("TranslationWorkPolicy.mayContinue", self.ui)
        self.assertIn("CaptionRetentionPolicy.oldestToDrop", self.ui)
        self.assertIn("OfflineSpeechLanguageSwitchPolicy.resolveDetectedTextSource(", self.ui)
        self.assertIn("!voiceSessions.isCurrent(voiceToken)", self.ui)

    def test_accessibility_word_lookup_and_safe_bounded_input(self):
        for marker in ("detectTapGestures", "getOffsetForPosition(point)",
                       "wordAt(text, it)", "onClick(label = \"Open lokale woordkaart\")",
                       "Outline", "selectedWord = it.take(40)",
                       "LocalGlossary.lookup(language, selectedWord)"):
            if marker == "Outline":
                marker = "OutlinedTextField("
            self.assertIn(marker, self.word)
        self.assertIn("Geen gecontroleerde lokale betekenis", self.word)
        self.assertIn("geen AI-gok".lower(), self.word.lower())

    def test_caption_wordcard_has_no_network_or_transcript_persistence(self):
        for bad in ("http://", "https://", "Socket(", "Retrofit", "Translation.getClient(",
                    "FileOutputStream", "SharedPreferences", "SpeechRecognizer"):
            self.assertNotIn(bad, self.word)
        self.assertIn("if (dialogOpen)", self.word)
        self.assertIn("dialogOpen = false", self.word)

if __name__ == "__main__":
    unittest.main()
