"""Canonical v0.4 archive import contract for the offline conversation model.

These pinned digests refer to the user's preserved
/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip.
The original sources and tests were imported byte-identically.
If intentionally evolving the model, update this provenance contract with
new tests and a documented import-vs-product delta.
"""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = {
    "app/src/main/java/com/livelingo/android/model/Conversation.kt":
        "1753d94d7e4154a00224027459ef8654c401fb246d121afe75dc3b1475369d83",
    "app/src/test/java/com/livelingo/android/model/ConversationUnitTest.kt":
        "4c8641b1518964e7f3383bb99cbfb704bcef040334ea958b8c44ecd6ee8d7f4c",
    "app/src/main/java/com/livelingo/android/model/CommunityPhrasebook.kt":
        "65fab2789ba250a58352dafdd6f013b227dee9b0baa157c2907d570754fe3321",
}


class OriginalV04Provenance(unittest.TestCase):
    def test_archived_models_and_acceptance_have_exact_expected_sha256(self):
        for path, expected in FIXTURES.items():
            with self.subTest(path=path):
                content = (ROOT / path).read_bytes()
                self.assertEqual(hashlib.sha256(content).hexdigest(), expected)
                self.assertGreater(len(content), 100)

    def test_separate_original_archived_model_is_not_claimed_as_live_ui(self):
        app = (ROOT / "app/src/main/java/com/livelingo/android/MainActivity.kt").read_text(encoding="utf-8")
        self.assertNotIn("Conversation().", app)
        self.assertIn("CaptionRetentionPolicy.oldestToDrop", app)
        self.assertIn("TranslationWorkPolicy.mayContinue", app)


if __name__ == "__main__":
    unittest.main()
