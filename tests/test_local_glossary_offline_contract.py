"""Offline glossary may only offer curated, source-backed definitions."""
from pathlib import Path
import unittest

APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/livelingo/android/model"
TEST = Path(__file__).resolve().parents[1] / "app/src/test/java/com/livelingo/android/model"

class LocalGlossaryQualityGate(unittest.TestCase):
    def test_no_provider_calls_or_claimed_languages_without_curated_entries(self):
        code = (APP / "LocalGlossary.kt").read_text(encoding="utf-8")
        self.assertIn("return entries[code]?.get(normalized)", code)
        self.assertIn("languageTag?.trim()?.lowercase(Locale.ROOT)", code)
        for unsafe in ("http://", "https://", "Socket(", "File(", "SpeechRecognizer",
                       "Translation.getClient", "createOnDeviceSpeechRecognizer", "SharedPreferences"):
            self.assertNotIn(unsafe, code)
        for lang in ('"en"', '"de"', '"nl"', '"fr"', '"es"'):
            self.assertIn(lang + " to mapOf(", code)
        for unsupported in ('"kmr" to mapOf(', '"ckb" to mapOf(', '"ps" to mapOf('):
            self.assertNotIn(unsupported, code)

    def test_native_acceptance_denies_unverified_translation_claims(self):
        tests = (TEST / "LocalGlossaryTest.kt").read_text(encoding="utf-8")
        for case in ('"quasar"', '"medical-legal-advice"', '"medicine"', '"ckb"', '"kmr"'):
            self.assertIn(case, tests)
        self.assertIn("assertNull(LocalGlossary.lookup", tests)


if __name__ == "__main__":
    unittest.main()
