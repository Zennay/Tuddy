#!/usr/bin/env python3
"""Independently re-download a current-main Android artifact on a self-hosted runner.

Proves ZIP/APK build provenance only. Does not attest phone installation, radio
isolation, speech quality, signing or release admission.
"""
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

REPO = "Zennay/Tuddy"
API = "https://api.github.com/repos/" + REPO + "/"
USER_AGENT = "Tuddy-self-hosted-artifact-integrity"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def api_request(path: str) -> urllib.request.Request:
    token = os.environ.get("GITHUB_TOKEN", "")
    require(bool(token), "GITHUB_TOKEN is not available to this read-only job.")
    return urllib.request.Request(
        API + path,
        headers={
            "Authorization": "Bearer " + token,
            "Accept": "application/vnd.github+json",
            "User-Agent": USER_AGENT,
        },
    )


def api_json(path: str) -> dict:
    with urllib.request.urlopen(api_request(path), timeout=45) as response:
        return json.load(response)


class RefuseRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        return None


def signed_artifact_url(artifact_id: int) -> str:
    # Do not forward an authorization header to GitHub's signed blob URL.
    opener = urllib.request.build_opener(RefuseRedirect)
    try:
        response = opener.open(
            api_request(f"actions/artifacts/{artifact_id}/zip"), timeout=45
        )
    except urllib.error.HTTPError as error:
        if error.code not in (301, 302, 303, 307, 308):
            raise RuntimeError(
                f"GitHub artifact download API returned HTTP {error.code}."
            ) from None
        target = error.headers.get("Location", "")
    else:
        response.close()
        raise RuntimeError("GitHub artifact API did not return a signed redirect.")
    parsed = urllib.parse.urlsplit(target)
    require(
        parsed.scheme == "https"
        and bool(parsed.hostname)
        and parsed.username is None
        and parsed.password is None,
        "GitHub artifact API returned an invalid signed download URL.",
    )
    return target


def download_verified_zip(url: str, target: Path, expected_digest: str, expected_bytes: int):
    sha = hashlib.sha256()
    length = 0
    # Deliberately unauthenticated: the signed link already grants this read.
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as source:
        with target.open("wb") as destination:
            while True:
                chunk = source.read(1024 * 1024)
                if not chunk:
                    break
                length += len(chunk)
                require(length <= expected_bytes, "Downloaded ZIP exceeded GitHub's declared size.")
                sha.update(chunk)
                destination.write(chunk)
    require(length == expected_bytes, "Downloaded ZIP size differs from GitHub artifact metadata.")
    require(sha.hexdigest() == expected_digest, "Downloaded ZIP SHA-256 differs from GitHub artifact metadata.")
    return length


def read_apk_checksum_from_zip(archive: Path) -> str:
    with zipfile.ZipFile(archive) as zipped:
        sidecars = [
            info for info in zipped.infolist()
            if info.filename.rsplit("/", 1)[-1] == "app-debug.apk.sha256"
        ]
        require(len(sidecars) == 1, "Expected exactly one APK SHA-256 sidecar in downloaded ZIP.")
        require(sidecars[0].file_size <= 128, "APK SHA-256 sidecar exceeds 128 bytes.")
        with zipped.open(sidecars[0]) as checksum_file:
            sidecar = checksum_file.read(129).decode("ascii")
    matched = re.fullmatch(r"([0-9a-f]{64})  app-debug[.]apk\n?", sidecar)
    require(matched is not None, "Downloaded APK sidecar has unexpected format.")
    return matched.group(1)


def main() -> None:
    require(
        os.environ.get("GITHUB_REPOSITORY") == REPO,
        "The verifier must run on the actual Tuddy repository.",
    )
    current_sha = api_json("commits/main")["sha"]
    require(re.fullmatch(r"[0-9a-f]{40}", current_sha) is not None, "Invalid current main SHA.")
    require(
        current_sha == os.environ.get("GITHUB_SHA"),
        "The verification checkout is not current main; refuse a stale artifact.",
    )

    runs = api_json(
        "actions/runs?branch=main&event=push&status=success&per_page=100"
    )["workflow_runs"]
    matching = [
        run for run in runs
        if run.get("head_sha") == current_sha
        and run.get("head_branch") == "main"
        and run.get("event") == "push"
        and run.get("conclusion") == "success"
        and run.get("name") == "Android APK (self-hosted)"
    ]
    require(matching, "No successful self-hosted Android main-push build for current main.")
    run_id = matching[0]["id"]

    artifacts = api_json(f"actions/runs/{run_id}/artifacts")["artifacts"]
    matching_artifacts = [
        artifact for artifact in artifacts
        if artifact.get("name") == "Tuddy-debug-apk"
        and not artifact.get("expired", True)
    ]
    require(len(matching_artifacts) == 1, "Expected one unexpired APK artifact for the selected build.")
    artifact = matching_artifacts[0]
    digest = artifact.get("digest", "")
    require(re.fullmatch(r"sha256:[0-9a-f]{64}", digest) is not None, "Missing GitHub SHA-256 artifact digest.")
    expected_zip_bytes = artifact.get("size_in_bytes")
    require(
        type(expected_zip_bytes) is int and 0 < expected_zip_bytes < 512 * 1024 * 1024,
        "Artifact ZIP metadata is missing or unexpectedly large.",
    )

    with tempfile.TemporaryDirectory(prefix="tuddy-apk-integrity-") as folder:
        archive = Path(folder) / "artifact.zip"
        download_verified_zip(
            signed_artifact_url(artifact["id"]),
            archive,
            digest.removeprefix("sha256:"),
            expected_zip_bytes,
        )
        apk_sha = read_apk_checksum_from_zip(archive)
        verify = Path("scripts/verify-tuddy-apk-artifact.sh")
        require(verify.is_file(), "Pinned ZIP/APK verifier script is missing.")
        environment = os.environ.copy()
        environment["TMPDIR"] = folder
        subprocess.run(
            ["bash", str(verify), str(archive), digest.removeprefix("sha256:"),
             apk_sha, current_sha, str(run_id)],
            check=True, env=environment,
        )

    require(
        api_json("commits/main")["sha"] == current_sha,
        "main advanced during artifact verification; retry on the new main.",
    )
    artifact_url = (
        f"https://github.com/{REPO}/actions/runs/{run_id}/artifacts/{artifact['id']}"
    )
    print(f"INDEPENDENT_DOWNLOAD_VERIFIED source={current_sha} run={run_id}")
    print(f"artifact={artifact_url}")
    print(f"zip_sha256={digest.removeprefix('sha256:')}")
    print(f"apk_sha256={apk_sha}")
    print("Physical Android 12+ isolation and 10+10 speech/MT observations remain OPEN.")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as output:
            output.write(
                "## Tuddy current-main independent artifact re-download\n\n"
                f"- Source: `{current_sha}`\n"
                f"- [GitHub APK artifact]({artifact_url})\n"
                f"- ZIP SHA-256: `{digest.removeprefix('sha256:')}`\n"
                f"- APK SHA-256: `{apk_sha}`\n"
                "- ZIP downloaded again from GitHub on a self-hosted runner, "
                "SHA/bytes/manifest/sidecar/APK verified.\n"
                "- **NOT** physical Android offline acceptance, signing or release admission.\n"
            )


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"::error::Tuddy artifact verification failed: {error}", file=sys.stderr)
        sys.exit(1)
