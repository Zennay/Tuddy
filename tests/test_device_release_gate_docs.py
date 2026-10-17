"""Fail closed on Tuddy's physical Android validation status.

A green GitHub build or artifact checksum alone must never be rephrased as
offline handset acceptance, licensed Kurdish/Pashto speech, or deployment.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
DEVICE = ROOT / "docs/DEVICE_VALIDATION.md"
APK_DIGEST = "fdc2dc8d4ec7de7293afca9ce3ed1aad832e21c3d0ba1aedef2210bc3b120bf6"
SOURCE_SHA = "b1a298a907f8ff2f7459b9c934f56fafb462f502"

class DeviceReleaseGateDocs(unittest.TestCase):
    def test_readme_does_not_conflate_apk_build_with_physical_validation(self):
        text = README.read_text(encoding="utf-8")
        self.assertIn(APK_DIGEST, text)
        self.assertIn(SOURCE_SHA, text)
        self.assertIn("39064114978", text)
        self.assertIn("NOT device-validated", text)
        self.assertIn("not constitute a", text.lower() + " not constitute a")
        self.assertIn("five-phrase", text)
        self.assertIn("Kurmanji", text)
        self.assertIn("Sorani", text)
        self.assertIn("Pashto", text)
        self.assertIn("Full", text)

    def test_device_acceptance_requires_real_observations_not_hosted_ci(self):
        text = DEVICE.read_text(encoding="utf-8")
        for token in (SOURCE_SHA, APK_DIGEST, "12112063195", "app-debug.apk",
                      "source-commit.txt", "10", "500", "TalkBack", "airplane mode",
                      "Stop", "850ms", "350ms", "NOT RUN", "NOT PROVIDED",
                      "Human acceptance / owner sign-off", "Release-ready: NO",
                      "zCloud runtime admission: NO"):
            self.assertIn(token, text)
        self.assertIn("do not submit raw private", text.lower())
        self.assertIn("issue", text.lower())
        self.assertNotRegex(text, r"(?i)release-ready:\s*(?:yes|true)")
        self.assertNotRegex(text, r"(?i)human acceptance[^\n]*:\s*(?:granted|yes)")

if __name__ == "__main__":
    unittest.main()
