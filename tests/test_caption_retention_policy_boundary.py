"""Offline regression: asynchronous captions remain bounded and private."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"


class CaptionMemoryBoundary(unittest.TestCase):
    def test_eviction_runs_before_async_translation(self):
        ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        add = ui.index('lines.add(Line(id, sentence, "Vertalen…", "Bezig"))')
        evict = ui.index("repeat(CaptionRetentionPolicy.oldestToDrop(lines.size))")
        remove = ui.index("lines.removeAt(0)", evict)
        translate = ui.index("translate(sentence, id, selectedTarget, selectedSource, token)", add)
        self.assertLess(add, evict)
        self.assertLess(evict, remove)
        self.assertLess(remove, translate)
        self.assertIn("lines.any { it.id == id }", ui)

    def test_small_bounded_ram_only_policy_does_not_write_disk(self):
        policy = (APP / "CaptionRetentionPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("MAX_CAPTIONS = 500", policy)
        self.assertIn("(count - MAX_CAPTIONS).coerceAtLeast(0)", policy)
        for unsafe in ("File(", "FileOutputStream", "SharedPreferences", "RoomDatabase",
                       "ContentResolver", "http://", "https://", "Socket("):
            self.assertNotIn(unsafe, policy)

    def test_follow_live_reacts_to_new_last_id_even_when_count_stays_500(self):
        ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("LaunchedEffect(lines.lastOrNull()?.id, partial.isNotBlank(), follow)", ui)
        self.assertNotIn("LaunchedEffect(lines.size, follow)", ui)

    def test_existing_session_clear_still_removes_every_caption(self):
        ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("lines.clear()", ui)
        self.assertIn("voiceSessions.invalidate()", ui)
        self.assertIn("generation++", ui)


if __name__ == "__main__":
    unittest.main()
