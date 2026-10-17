"""Archived v0.4 Unicode tap-to-word port is strictly local and explicit."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/livelingo/android/model/WordSelection.kt"
TEST = ROOT / "app/src/test/java/com/livelingo/android/model/WordSelectionTest.kt"


class WordSelectionSourceGate(unittest.TestCase):
    def test_bounded_character_index_and_punctuation_policy(self):
        src = SRC.read_text(encoding="utf-8")
        self.assertIn("if (characterOffset !in text.indices) return null", src)
        self.assertIn("c.isLetter()", src)
        self.assertIn("return text.substring(left, right)", src)
        for prohibited in ("http:", "https:", "fetch(", "TranslatorOptions",
                           "SpeechRecognizer", "FileOutputStream", "SharedPreferences"):
            self.assertNotIn(prohibited, src)

    def test_multiscript_jvm_acceptance_is_registered(self):
        text = TEST.read_text(encoding="utf-8")
        for sample in ("سڵاو", "پښتو", "Verstehst", "l’amour", "co-operate"):
            self.assertIn(sample, text)
        self.assertIn("assertNull(wordAt", text)


if __name__ == "__main__":
    unittest.main()
