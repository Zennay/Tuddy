#!/usr/bin/env python3
"""Inspect the *built APK's merged* AndroidManifest, not just source XML.

Only reads the local APK via Android SDK apkanalyzer. Fails closed before
publishing a debug artifact if package identity, permission or export/privacy
posture has drifted through Gradle dependencies or source changes. A green
result is source/package acceptance, NOT physical offline speech validation.
"""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_PACKAGE = "com.livelingo.android"
FORBIDDEN_PERMISSIONS = {
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    "android.permission.CAMERA",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.READ_SMS",
    "android.permission.SEND_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.QUERY_ALL_PACKAGES",
}


def local_name(tag):
    return tag.rsplit("}", 1)[-1]


def permitted_exported_component(kind, name, permission):
    # AndroidX ProfileInstaller's debug/performance tooling exposes a receiver
    # guarded by the platform DUMP signature/privileged permission. It is not
    # callable by unprivileged third-party apps. Never allow it without DUMP.
    if kind == "activity" and name in (
        ".MainActivity", "com.livelingo.android.MainActivity"):
        return True
    return (kind == "receiver" and
            name == "androidx.profileinstaller.ProfileInstallReceiver" and
            permission == "android.permission.DUMP")


def audit_manifest(xml_text):
    problems = []
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return ["manifest_xml_invalid"]
    if local_name(root.tag) != "manifest":
        return ["manifest_root_invalid"]
    if root.get("package") != EXPECTED_PACKAGE:
        problems.append("package_identity_drift")
    if root.get(ANDROID + "sharedUserId"):
        problems.append("shared_uid_not_allowed")
    sdk = [child for child in root if local_name(child.tag) == "uses-sdk"]
    if len(sdk) != 1:
        problems.append("sdk_boundary_missing")
    else:
        for name, expected in (("minSdkVersion", 31), ("targetSdkVersion", 35)):
            value = sdk[0].get(ANDROID + name, "")
            try:
                actual = int(value, 10)
            except ValueError:
                actual = -1
            if actual != expected:
                problems.append(name + "_drift")
    permissions = {
        node.get(ANDROID + "name")
        for node in root
        if local_name(node.tag) in ("uses-permission", "uses-permission-sdk-23")
    }
    if "android.permission.RECORD_AUDIO" not in permissions:
        problems.append("record_audio_permission_missing")
    if FORBIDDEN_PERMISSIONS & permissions:
        problems.append("unexpected_sensitive_permission")
    apps = [child for child in root if local_name(child.tag) == "application"]
    if len(apps) != 1:
        problems.append("application_boundary_missing")
        return problems
    app = apps[0]
    if app.get(ANDROID + "allowBackup") != "false":
        problems.append("backup_not_explicitly_disabled")
    if app.get(ANDROID + "usesCleartextTraffic") != "false":
        problems.append("cleartext_not_explicitly_disabled")
    launchers = []
    for element in app.iter():
        kind = local_name(element.tag)
        if kind not in ("activity", "activity-alias", "service", "receiver", "provider"):
            continue
        name = element.get(ANDROID + "name", "")
        if element.get(ANDROID + "exported") == "true":
            if not permitted_exported_component(
                    kind, name, element.get(ANDROID + "permission")):
                problems.append("unexpected_exported_component")
        if kind == "activity" and name in (
            ".MainActivity", "com.livelingo.android.MainActivity"):
            for filter_node in element:
                if local_name(filter_node.tag) != "intent-filter":
                    continue
                actions = {child.get(ANDROID + "name") for child in filter_node
                           if local_name(child.tag) == "action"}
                categories = {child.get(ANDROID + "name") for child in filter_node
                              if local_name(child.tag) == "category"}
                if ("android.intent.action.MAIN" in actions and
                    "android.intent.category.LAUNCHER" in categories and
                    element.get(ANDROID + "exported") == "true"):
                    launchers.append(name)
    if len(launchers) != 1:
        problems.append("launcher_identity_drift")
    return sorted(set(problems))


def find_apkanalyzer():
    path = shutil.which("apkanalyzer")
    if path:
        return path
    android_home = os.getenv("ANDROID_HOME") or os.getenv("ANDROID_SDK_ROOT", "")
    for suffix in ("cmdline-tools/latest/bin/apkanalyzer", "tools/bin/apkanalyzer"):
        path = Path(android_home) / suffix
        if android_home and path.is_file():
            return str(path)
    raise RuntimeError("apkanalyzer_missing")


def verify_apk(apk_path):
    if not apk_path.is_file() or apk_path.stat().st_size == 0:
        raise RuntimeError("apk_missing_or_empty")
    completed = subprocess.run(
        [find_apkanalyzer(), "manifest", "print", str(apk_path)],
        text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=60, check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError("merged_manifest_extract_failed")
    problems = audit_manifest(completed.stdout)
    if problems:
        if "unexpected_exported_component" in problems:
            # Class names/manifest types are public APK metadata, not secrets.
            # Limit output to exact offending types/names for accountable review;
            # never print the full manifest or runtime transcript.
            root = ET.fromstring(completed.stdout)
            rejected = []
            for element in root.iter():
                kind = local_name(element.tag)
                if kind in ("activity", "activity-alias", "service", "receiver", "provider"):
                    if element.get(ANDROID + "exported") == "true":
                        name = element.get(ANDROID + "name", "")
                        if not permitted_exported_component(
                                kind, name, element.get(ANDROID + "permission")):
                            rejected.append(kind + ":" + name[:180])
            print("Unexpected APK exported components: " +
                  ", ".join(rejected[:10]), file=sys.stderr)
        raise RuntimeError(",".join(problems))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: check_android_apk_manifest.py <built-debug-apk>", file=sys.stderr)
        raise SystemExit(2)
    try:
        verify_apk(Path(sys.argv[1]))
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        print("APK merged-manifest privacy gate FAIL: " + str(error), file=sys.stderr)
        raise SystemExit(1)
    print("APK merged-manifest privacy, package and launcher gate PASS")
