"""The actual Android UI callsites must not render arbitrary ML Kit/ASR errors.

Offline synthetic/source test, not a physical microphone or provider test.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/livelingo/android"


class LocalErrorPrivacyBoundary(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui = (APP / "MainActivity.kt").read_text(encoding="utf-8")
        cls.policy = (APP / "SafeLocalFailureText.kt").read_text(encoding="utf-8")

    def test_translation_failure_uses_fixed_nonsecret_ui_copy(self):
        start = self.ui.index("translator.translate(text)")
        end = self.ui.index("translator.close()", self.ui.index(".addOnFailureListener", start))
        self.assertIn("updateLine(id, SafeLocalFailureText.TRANSLATION", self.ui[start:end])
        self.assertNotIn("localizedMessage", self.ui[start:end])

    def test_speech_start_error_never_displays_raw_exception_message(self):
        start = self.ui.index("        } catch (exception: Exception) {\n            // startListening may throw")
        end = self.ui.index("    val microphonePermission", start)
        failure = self.ui[start:end]
        self.assertIn("error = SafeLocalFailureText.SPEECH_START", failure)
        self.assertNotIn("exception.", failure)
        self.assertNotIn("localizedMessage", failure)

    def test_no_network_or_sensitive_data_persisted_in_policy(self):
        executable = self.policy.split("internal object SafeLocalFailureText", 1)[1]
        for forbidden in ("http://", "https://", "Throwable", "Exception", "FileOutputStream",
                          "SharedPreferences(", "SpeechRecognizer.create", "Translation.getClient(", "Log."):
            self.assertNotIn(forbidden, executable)
        self.assertIn("const val SPEECH_START", self.policy)
        self.assertIn("const val TRANSLATION", self.policy)
        self.assertNotIn("localizedMessage", self.ui)


if __name__ == "__main__":
    unittest.main()
