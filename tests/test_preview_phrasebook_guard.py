"""Prevent the archived v0.4 phrasebook from bypassing UI input hardening."""
import hashlib
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ARCHIVED = "app/src/main/java/com/livelingo/android/model/CommunityPhrasebook.kt"
ORIGINAL_SHA256 = "65fab2789ba250a58352dafdd6f013b227dee9b0baa157c2907d570754fe3321"


class PreviewGuardWiringContract(unittest.TestCase):
    def test_archived_dictionary_remains_immutable(self):
        self.assertEqual(hashlib.sha256((ROOT / ARCHIVED).read_bytes()).hexdigest(), ORIGINAL_SHA256)

    def test_the_actual_preview_button_invokes_guard_not_archived_lookup(self):
        ui = (ROOT / "app/src/main/java/com/livelingo/android/MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("import com.livelingo.android.model.SafeCommunityPhrasebook", ui)
        self.assertIn(
            "previewResult = SafeCommunityPhrasebook.translate(previewSource, previewTarget, previewInput)",
            ui,
        )
        self.assertNotIn("previewResult = CommunityPhrasebook.translate(", ui)

    def test_guard_is_local_bounded_and_does_not_introduce_network_or_models(self):
        path = ROOT / "app/src/main/java/com/livelingo/android/model/SafeCommunityPhrasebook.kt"
        src = path.read_text(encoding="utf-8")
        self.assertIn("MAX_PREVIEW_CHARS = 256", src)
        self.assertIn("Character.isISOControl", src)
        self.assertIn("Character.FORMAT", src)
        self.assertIn("Character.isSurrogate", src)
        self.assertIn("CommunityPhrasebook.translate(source, target, phrase)", src)
        for forbidden in ("HttpURLConnection", "SpeechRecognizer", "TranslatorOptions", "URL("):
            self.assertNotIn(forbidden, src)


if __name__ == "__main__":
    unittest.main()
