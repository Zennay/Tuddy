"""Ensure the new live lyric preview uses only local, bounded ML Kit text."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/livelingo/android"
UI = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
POLICY = (SRC / "InterimTranslationPolicy.kt").read_text(encoding="utf-8")


class LiveInterimTranslationQA(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.interim = UI.split("    fun prepareInterim(", 1)[1].split(
            "    // Queued work is valid only for this *exact*", 1)[0]

    def test_stable_runnable_replaces_old_and_uses_only_downloaded_models(self):
        section = self.interim
        for marker in ("pendingInterim?.let(interimHandler::removeCallbacks)",
                       "revision = ++partialRevision",
                       "InterimTranslationPolicy.eligible(",
                       "interimHandler.postDelayed(task, InterimTranslationPolicy.STABLE_WAIT_MS)",
                       "translator.translate(text)", "translator.close()"):
            self.assertIn(marker, section)
        self.assertNotIn("downloadModelIfNeeded(", section.replace(
            "// Do not call downloadModelIfNeeded: no per-word model traffic.", ""))
        self.assertNotIn("createSpeechRecognizer(", section)

    def test_repeated_partial_and_final_result_never_show_stale_preview(self):
        self.assertIn("if (latest != partial)", UI)
        self.assertIn("prepareInterim(latest, observedSource, selectedTarget, voiceToken, token)", UI)
        self.assertIn("clearInterim()", UI)
        self.assertGreaterEqual(UI.count("clearInterim()"), 4)
        self.assertIn("voiceSessions.isCurrent(voiceToken), listening", self.interim)
        self.assertIn("revision, partialRevision, token, generation", self.interim)
        self.assertIn("if (!stillCurrent || interimTranslationBusy)", self.interim)

    def test_live_translation_visible_and_scroll_follows_its_revision(self):
        self.assertIn("if (partialTranslation.isNotBlank())", UI)
        self.assertIn("Text(partialTranslation,", UI)
        self.assertIn(
            "LaunchedEffect(lines.lastOrNull()?.id, partial, partialTranslation, follow)", UI)
        self.assertIn("pendingInterim?.let(interimHandler::removeCallbacks)", UI)

    def test_source_policy_fails_closed_without_cloud_or_persistence(self):
        self.assertIn("STABLE_WAIT_MS = 650L", POLICY)
        self.assertIn("MAX_INTERIM_CHARS = 1500", POLICY)
        for name in ("SharedPreferences", "FileOutputStream", "RoomDatabase", "http://",
                     "https://", "Socket(", "downloadModelIfNeeded("):
            self.assertNotIn(name, POLICY)
        self.assertNotIn("localizedMessage", self.interim)


if __name__ == "__main__":
    unittest.main()
