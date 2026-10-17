"""Synthetic adversarial checks for built-APK manifest privacy admission.

The production workflow separately exercises apkanalyzer against the actual
merged APK; these tests validate the offline policy and its negative canaries.
"""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "tools/check_android_apk_manifest.py"
spec = importlib.util.spec_from_file_location("tuddy_apk_manifest_gate", SCRIPT)
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)

GOOD = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.livelingo.android">
  <uses-sdk android:minSdkVersion="31" android:targetSdkVersion="35" />
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.INTERNET" />
  <application android:allowBackup="false" android:usesCleartextTraffic="false">
    <activity android:name=".MainActivity" android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    <provider android:name="androidx.startup.InitializationProvider"
              android:exported="false" />
  </application>
</manifest>
"""


class MergedApkManifestGate(unittest.TestCase):
    def test_known_good_debug_manifest_passes(self):
        self.assertEqual([], policy.audit_manifest(GOOD))

    def test_package_sdk_and_launcher_identity_are_bound(self):
        for label, mutation, expected in (
            ("package", GOOD.replace('package="com.livelingo.android"',
                                     'package="com.evil.app"'), "package_identity_drift"),
            ("minimum sdk", GOOD.replace('minSdkVersion="31"', 'minSdkVersion="23"'), "minSdkVersion_drift"),
            ("target sdk", GOOD.replace('targetSdkVersion="35"', 'targetSdkVersion="28"'), "targetSdkVersion_drift"),
            ("missing launcher", GOOD.replace('android.intent.category.LAUNCHER',
                                               'android.intent.category.DEFAULT'), "launcher_identity_drift"),
        ):
            with self.subTest(label=label):
                self.assertIn(expected, policy.audit_manifest(mutation))

    def test_any_sensitive_permission_added_by_dependency_fails(self):
        for permission in sorted(policy.FORBIDDEN_PERMISSIONS):
            with self.subTest(permission=permission):
                replaced = GOOD.replace(
                    '  <application', '  <uses-permission android:name="' + permission + '" />\n  <application')
                self.assertIn("unexpected_sensitive_permission", policy.audit_manifest(replaced))

    def test_androidx_profileinstaller_requires_signature_dump_permission(self):
        permitted = GOOD.replace(
            "  </application>",
            '    <receiver android:name="androidx.profileinstaller.ProfileInstallReceiver" '
            'android:exported="true" android:permission="android.permission.DUMP" />\\n'
            "  </application>",
        )
        self.assertEqual([], policy.audit_manifest(permitted))
        for bad in (
            permitted.replace(' android:permission="android.permission.DUMP"', ""),
            permitted.replace('android.permission.DUMP', 'android.permission.INTERNET'),
            permitted.replace('androidx.profileinstaller.ProfileInstallReceiver',
                              'androidx.profileinstaller.UnrecognizedReceiver'),
        ):
            self.assertIn("unexpected_exported_component", policy.audit_manifest(bad))

    def test_missing_microphone_permission_denied(self):
        xml = GOOD.replace('<uses-permission android:name="android.permission.RECORD_AUDIO" />', "")
        self.assertIn("record_audio_permission_missing", policy.audit_manifest(xml))

    def test_backup_and_cleartext_must_be_explicitly_disabled(self):
        for flag, value, code in (
            ("allowBackup", "true", "backup_not_explicitly_disabled"),
            ("usesCleartextTraffic", "true", "cleartext_not_explicitly_disabled"),
        ):
            for mode in ("enabled", "missing"):
                with self.subTest(flag=flag, mode=mode):
                    xml = GOOD.replace('android:' + flag + '="false"',
                                       ('android:' + flag + '="' + value + '"') if mode == "enabled" else "")
                    self.assertIn(code, policy.audit_manifest(xml))

    def test_unexpected_exported_components_fail_closed(self):
        for component in ("service", "receiver", "provider", "activity-alias"):
            xml = GOOD.replace('  </application>', '    <' + component +
                               ' android:name="evil.Receiver" android:exported="true" />\n  </application>')
            self.assertIn("unexpected_exported_component", policy.audit_manifest(xml))

    def test_shared_uid_and_invalid_or_incomplete_xml_are_not_accepted(self):
        shared = GOOD.replace('package="com.livelingo.android"',
                              'package="com.livelingo.android" android:sharedUserId="legacy.uid"')
        self.assertIn("shared_uid_not_allowed", policy.audit_manifest(shared))
        self.assertIn("manifest_xml_invalid", policy.audit_manifest(GOOD[:-25]))
        self.assertIn("sdk_boundary_missing", policy.audit_manifest(
            GOOD.replace('  <uses-sdk android:minSdkVersion="31" android:targetSdkVersion="35" />', "")))

    def test_workflow_invokes_gate_on_compiled_apk_before_upload(self):
        workflow = (SCRIPT.parents[1] / ".github/workflows/android.yml").read_text(encoding="utf-8")
        inv = 'python3 -B tools/check_android_apk_manifest.py "app/build/outputs/apk/debug/app-debug.apk"'
        self.assertIn(inv, workflow)
        self.assertLess(workflow.index(inv), workflow.index('actions/upload-artifact@v4'))
        self.assertLess(workflow.index("gradle :app:testDebugUnitTest :app:assembleDebug"), workflow.index(inv))
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer", SCRIPT.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
