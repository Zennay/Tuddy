"""Synthetic source-level guard for continuous on-device voice sessions.

This test proves the exact state machine is wired into the Activity; Kotlin
JUnit separately validates bounded retry admission. It is *not* a substitute
for a physical Android airplane-mode microphone demonstration.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"
UI = APP / "MainActivity.kt"


class ContinuousOfflineVoiceBoundary(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = UI.read_text(encoding="utf-8")
        cls.policy = (APP / "ContinuousSpeechRestartPolicy.kt").read_text(encoding="utf-8")

    def test_restarts_after_final_and_expected_silence_only(self):
        self.assertIn("fun start(forceRestart: Boolean = false)", self.ui)
        self.assertIn("if (listening && !forceRestart) return", self.ui)
        self.assertIn("start(forceRestart = true)", self.ui)
        self.assertIn("ContinuousSpeechRestartPolicy.RESULT_DELAY_MS", self.ui)
        self.assertIn("SpeechRecognizer.ERROR_NO_MATCH", self.ui)
        self.assertIn("SpeechRecognizer.ERROR_SPEECH_TIMEOUT", self.ui)
        self.assertIn("ContinuousSpeechRestartPolicy.delayForError(", self.ui)
        self.assertIn("stop() // never retry permissions", self.ui)
        self.assertIn("if (sentence.isNotBlank() && token == generation)", self.ui)
        self.assertNotIn("listening = false\n                    val sentence", self.ui)

    def test_restart_never_survives_stop_background_or_permission_revocation(self):
        self.assertIn("voiceSessions.invalidate()", self.ui)
        self.assertIn("pendingRestart?.let(restartHandler::removeCallbacks)", self.ui)
        self.assertIn("pendingRestart = null", self.ui)
        self.assertIn("if (voiceSessions.isCurrent(voiceToken) && listening &&", self.ui)
        self.assertIn("MicrophoneStartPolicy.mayStart(lifecycleOwner.lifecycle.currentState)", self.ui)
        self.assertIn("ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)", self.ui)
        self.assertIn("Lifecycle.Event.ON_STOP) stop()", self.ui)
        self.assertIn("onDispose {", self.ui)
        self.assertIn("SpeechCleanupPolicy.release(active::cancel, active::destroy)", self.ui)
        self.assertIn("SpeechCleanupPolicy.release(previous::cancel, previous::destroy)", self.ui)
        self.assertIn("SpeechCleanupPolicy.release(failed::cancel, failed::destroy)", self.ui)

    def test_no_cloud_fallback_or_unbounded_fatal_error_retry(self):
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer(context)", self.ui)
        self.assertIn("RecognizerIntent.EXTRA_PREFER_OFFLINE, true", self.ui)
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(context)", self.ui)
        self.assertIn("if (retry != null)", self.ui)
        self.assertIn("else {\n                        stop()", self.ui)
        self.assertIn("return if (error == noMatch || error == speechTimeout)", self.policy)
        self.assertNotIn("http://", self.policy)
        self.assertNotIn("https://", self.policy)


if __name__ == "__main__":
    unittest.main()
