"""Quality gate: live lyrics must follow updates to an already visible interim line.

The old effect uses partial.isNotBlank() as a key. Once nonblank, every
additional word preserves that Boolean and the coroutine never scrolls to
the bottom of a growing partial caption. React to actual text revisions.
"""
from pathlib import Path
import re
import unittest

UI = Path(__file__).resolve().parents[1] / (
    "app/src/main/java/com/livelingo/android/MainActivity.kt"
)

class InterimGrowthFollowTest(unittest.TestCase):
    def test_effect_is_keyed_by_actual_interim_text(self):
        text = UI.read_text(encoding="utf-8")
        self.assertRegex(
            text,
            r"LaunchedEffect\(lines\.lastOrNull\(\)\?\.id,\s*partial,\s*partialTranslation,\s*follow\)",
        )
        self.assertNotIn("LaunchedEffect(lines.lastOrNull()?.id, partial.isNotBlank(), follow)", text)

    def test_continuous_interim_scroll_is_immediate_and_respects_pause(self):
        text = UI.read_text(encoding="utf-8")
        self.assertIn("LiveFollowPolicy.targetIndex(lines.size, partial.isNotBlank(), follow)", text)
        self.assertRegex(text, r"if \(index != null\)\s*\{")
        self.assertIn("if (partial.isNotBlank()) listState.scrollToItem(index)", text)
        self.assertIn("else listState.animateScrollToItem(index)", text)
        self.assertIn("if (!followEnabled) return null",
            (UI.parent / "LiveFollowPolicy.kt").read_text(encoding="utf-8"))

if __name__ == "__main__":
    unittest.main()
