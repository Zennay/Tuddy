"""No-device check for Tuddy's first/interim caption follow-live behavior."""
from pathlib import Path
import unittest

APP = Path(__file__).resolve().parents[1] / "app/src/main/java/com/livelingo/android"


class InterimFollowBoundary(unittest.TestCase):
    def test_interim_visibility_participates_in_follow_live_effect(self):
        ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("LaunchedEffect(lines.lastOrNull()?.id, partial, follow)", ui)
        self.assertIn("LiveFollowPolicy.targetIndex(lines.size, partial.isNotBlank(), follow)", ui)
        self.assertNotIn("LaunchedEffect(lines.size, follow)", ui)
        self.assertIn("if (partial.isNotBlank())", ui)
        self.assertIn("if (index != null) {", ui)
        self.assertIn("if (partial.isNotBlank()) listState.scrollToItem(index)", ui)
        self.assertIn("else listState.animateScrollToItem(index)", ui)

    def test_policy_never_scrolls_when_paused_or_empty(self):
        model = (APP / "LiveFollowPolicy.kt").read_text(encoding="utf-8")
        self.assertIn("if (!followEnabled) return null", model)
        self.assertIn("hasPartial -> finalCount", model)
        self.assertIn("finalCount > 0 -> finalCount - 1", model)
        self.assertIn("else -> null", model)
        self.assertNotIn("http", model)

    def test_final_and_interim_use_distinct_lazy_items(self):
        ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("items(lines, key = { it.id })", ui)
        self.assertIn("if (partial.isNotBlank())", ui)
        self.assertIn("item {", ui)


if __name__ == "__main__":
    unittest.main()
