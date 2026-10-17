"""Offline Android recognizer lifecycle source acceptance.

These assertions bind the JVM-tested release policy to the actual Compose
activity stop, forced restart, failure and disposal routes. They do not claim
that a real Android speech service or airplane-mode handset was exercised.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/livelingo/android"
TESTS = ROOT / "app/src/test/java/com/livelingo/android"


class SpeechRecognizerReleaseQA(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        cls.cleanup = (SRC / "SpeechCleanupPolicy.kt").read_text(encoding="utf-8")
        cls.jvm = (TESTS / "SpeechCleanupPolicyTest.kt").read_text(encoding="utf-8")

    def test_stop_invalidates_before_cancel_destroy_and_clears_reference(self):
        body = self.ui.split("    fun stop() {", 1)[1].split("\n    fun start(", 1)[0]
        ordered = (
            "voiceSessions.invalidate()",
            "pendingRestart?.let(restartHandler::removeCallbacks)",
            "pendingRestart = null",
            "val active = recognizer",
            "recognizer = null",
            "SpeechCleanupPolicy.release(active::cancel, active::destroy)",
            "listening = false",
            'partial = ""',
        )
        positions = [body.index(needle) for needle in ordered]
        self.assertEqual(positions, sorted(positions))

    def test_force_restart_releases_old_generation_before_allocation(self):
        section = self.ui.split("    fun start(forceRestart: Boolean = false) {", 1)[1]
        section = section.split("    val microphonePermission", 1)[0]
        ordered = (
            "val voiceToken = voiceSessions.begin()",
            "val previous = recognizer",
            "recognizer = null",
            "SpeechCleanupPolicy.release(previous::cancel, previous::destroy)",
            "SpeechRecognizer.createOnDeviceSpeechRecognizer(context)",
        )
        positions = [section.index(needle) for needle in ordered]
        self.assertEqual(positions, sorted(positions))
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(context)", section)

    def test_start_exception_invalidates_callbacks_and_releases_failed_instance(self):
        start = self.ui.split("    fun start(forceRestart: Boolean = false) {", 1)[1]
        failure = start.split("        } catch (exception: Exception) {", 1)[1]
        failure = failure.split("    val microphonePermission", 1)[0]
        ordered = (
            "voiceSessions.invalidate()",
            "pendingRestart?.let(restartHandler::removeCallbacks)",
            "pendingRestart = null",
            "val failed = recognizer",
            "recognizer = null",
            "SpeechCleanupPolicy.release(failed::cancel, failed::destroy)",
            "listening = false",
            'partial = ""',
        )
        positions = [failure.index(needle) for needle in ordered]
        self.assertEqual(positions, sorted(positions))

    def test_background_and_dispose_release_via_stop_without_resurrection(self):
        self.assertIn("Lifecycle.Event.ON_STOP) stop()", self.ui)
        on_dispose = self.ui.split("        onDispose {", 1)[1]
        on_dispose = on_dispose.split("    // The partial caption", 1)[0]
        self.assertIn("generation++", on_dispose)
        self.assertIn("stop()", on_dispose)
        self.assertNotIn("SpeechRecognizer.create", on_dispose)
        self.assertIn("SpeechCleanupPolicy.release", self.ui)
        self.assertIn("RecognizerIntent.EXTRA_PREFER_OFFLINE, true", self.ui)

    def test_release_boundary_and_jvm_failing_canaries_are_present(self):
        self.assertIn("fun release(cancel: () -> Unit, destroy: () -> Unit)", self.cleanup)
        self.assertRegex(self.cleanup, r"(?s)try\s*\{\s*cancel\(\).*?catch.*?try\s*\{\s*destroy\(\)")
        for check in (
            "releaseAlwaysCancelsBeforeDestroying",
            "disconnectedCancelStillDestroysRecognizer",
            "destroyFailureDoesNotEscapeStopOrStartExceptionCleanup",
            "bothFailWithoutSkippingEitherOrResumingAnything",
            "independentlyReusableAcrossMultipleSessions",
        ):
            self.assertIn(check, self.jvm)
        self.assertNotIn("http://", self.cleanup)
        self.assertNotIn("https://", self.cleanup)


if __name__ == "__main__":
    unittest.main()
