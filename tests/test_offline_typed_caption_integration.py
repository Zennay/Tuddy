"""Bind manual text entry to the shipped RAM-only, on-device translation path."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"
UI = (APP / "MainActivity.kt").read_text(encoding="utf-8")


class ManualOfflineTranslationBoundary(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manual = UI.split("// A local text-only fallback", 1)[1].split(
            "Row(horizontalArrangement = Arrangement.spacedBy(8.dp)", 1)[0]
        cls.policy = (APP / "OfflineTypedCaptionPolicy.kt").read_text(encoding="utf-8")

    def test_manual_text_reuses_the_actual_native_mlkit_translator(self):
        self.assertIn('Text("Vertaal tekst lokaal")', self.manual)
        self.assertIn("OfflineTypedCaptionPolicy.normalize(manualText)", self.manual)
        self.assertIn("translate(sentence, id, selectedTarget, selectedSource, token)", self.manual)
        self.assertIn("Translation.getClient(", UI)
        self.assertIn("DownloadConditions.Builder().requireWifi().build()", UI)
        self.assertNotIn("SpeechRecognizer.", self.manual)
        self.assertNotIn("microphonePermission", self.manual)

    def test_caption_has_correct_local_language_and_bounded_ram_eviction(self):
        markers = (
            'lines.add(Line(id, sentence, "Vertalen…", "Bezig",',
            "repeat(CaptionRetentionPolicy.oldestToDrop(lines.size))",
            "lines.removeAt(0)",
            'manualText = ""',
            "translate(sentence, id, selectedTarget, selectedSource, token)",
        )
        positions = [self.manual.index(marker) for marker in markers]
        self.assertEqual(sorted(positions), positions)

    def test_control_characters_or_overlong_inputs_never_enter_queue(self):
        self.assertIn("MAX_INPUT_UNITS = 1500", self.policy)
        self.assertIn("Character.isISOControl(it)", self.policy)
        self.assertIn("return null", self.policy)
        self.assertIn("candidate.length <= OfflineTypedCaptionPolicy.MAX_INPUT_UNITS", self.manual)
        self.assertIn("enabled = OfflineTypedCaptionPolicy.normalize(manualText) != null", self.manual)

    def test_clear_discards_typed_text_as_well_as_all_history(self):
        self.assertIn('manualText = ""', UI.split('lines.clear()', 1)[1])
        self.assertIn("generation++", UI)
        for forbidden in ("SharedPreferences", "FileOutputStream", "http://", "https://"):
            self.assertNotIn(forbidden, self.policy)


if __name__ == "__main__":
    unittest.main()
