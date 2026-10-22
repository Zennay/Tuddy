#!/usr/bin/env bash
# Verify the downloaded Tuddy GitHub artifact ZIP and the APK it contains.
# This is an artifact-integrity helper, not evidence of physical-device acceptance.
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  bash scripts/verify-tuddy-apk-artifact.sh ARTIFACT.zip EXPECTED_ZIP_SHA256 EXPECTED_APK_SHA256 [EXPECTED_SOURCE_COMMIT] [EXPECTED_WORKFLOW_RUN_ID]

The ZIP and APK digests are different values. Optional source/run arguments are
checked against source-commit.txt and build-manifest.txt inside the artifact.
USAGE
  exit 2
}

[[ $# -ge 3 && $# -le 5 ]] || usage
artifact_zip=$(realpath -- "$1")
expected_zip_sha=$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]')
expected_apk_sha=$(printf '%s' "$3" | tr '[:upper:]' '[:lower:]')
expected_source_sha="${4:-}"
expected_run_id="${5:-}"

for cmd in sha256sum unzip find realpath awk; do
  command -v "$cmd" >/dev/null 2>&1 || {
    printf 'Required command not found: %s\n' "$cmd" >&2
    exit 1
  }
done

[[ -f "$artifact_zip" ]] || { printf 'Artifact ZIP not found: %s\n' "$artifact_zip" >&2; exit 1; }
[[ "$expected_zip_sha" =~ ^[a-f0-9]{64}$ ]] || { printf 'Expected ZIP SHA-256 must be 64 hex characters.\n' >&2; exit 2; }
[[ "$expected_apk_sha" =~ ^[a-f0-9]{64}$ ]] || { printf 'Expected APK SHA-256 must be 64 hex characters.\n' >&2; exit 2; }

actual_zip_sha=$(sha256sum -- "$artifact_zip" | awk '{print $1}')
if [[ "$actual_zip_sha" != "$expected_zip_sha" ]]; then
  printf 'ZIP SHA-256 mismatch\nExpected: %s\nActual:   %s\n' "$expected_zip_sha" "$actual_zip_sha" >&2
  exit 1
fi
printf 'Artifact ZIP SHA-256: OK (%s)\n' "$actual_zip_sha"

# Refuse path-traversal entries before extracting a downloaded archive.
while IFS= read -r entry; do
  case "$entry" in
    /*|../*|*/../*|*/..|[A-Za-z]:/*)
      printf 'Unsafe path in artifact ZIP: %s\n' "$entry" >&2
      exit 1
      ;;
  esac
done < <(unzip -Z1 "$artifact_zip")

tmp_dir=$(mktemp -d)
trap 'rm -rf -- "$tmp_dir"' EXIT
unzip -q "$artifact_zip" -d "$tmp_dir"

mapfile -t apks < <(find "$tmp_dir" -type f -name app-debug.apk -print)
[[ ${#apks[@]} -eq 1 ]] || {
  printf 'Expected exactly one app-debug.apk in artifact; found %s.\n' "${#apks[@]}" >&2
  exit 1
}
apk="${apks[0]}"
sidecar="${apk}.sha256"
[[ -f "$sidecar" ]] || { printf 'APK checksum sidecar is missing.\n' >&2; exit 1; }
(cd "$(dirname "$apk")" && sha256sum --check "$(basename "$sidecar")")

actual_apk_sha=$(sha256sum -- "$apk" | awk '{print $1}')
if [[ "$actual_apk_sha" != "$expected_apk_sha" ]]; then
  printf 'APK SHA-256 mismatch\nExpected: %s\nActual:   %s\n' "$expected_apk_sha" "$actual_apk_sha" >&2
  exit 1
fi
printf 'APK SHA-256: OK (%s)\n' "$actual_apk_sha"

mapfile -t source_files < <(find "$tmp_dir" -type f -name source-commit.txt -print)
mapfile -t manifests < <(find "$tmp_dir" -type f -name build-manifest.txt -print)
[[ ${#source_files[@]} -eq 1 ]] || { printf 'Expected exactly one source-commit.txt.\n' >&2; exit 1; }
[[ ${#manifests[@]} -eq 1 ]] || { printf 'Expected exactly one build-manifest.txt.\n' >&2; exit 1; }

source_sha=$(tr -d '[:space:]' < "${source_files[0]}")
manifest="${manifests[0]}"
grep -Fxq "apk_sha256=$expected_apk_sha" "$manifest" || {
  printf 'Build manifest APK digest does not match the requested APK digest.\n' >&2
  exit 1
}
if [[ -n "$expected_source_sha" ]]; then
  [[ "$source_sha" == "$expected_source_sha" ]] || {
    printf 'Source commit mismatch\nExpected: %s\nActual:   %s\n' "$expected_source_sha" "$source_sha" >&2
    exit 1
  }
fi
if [[ -n "$expected_run_id" ]]; then
  grep -Fxq "workflow_run_id=$expected_run_id" "$manifest" || {
    printf 'Workflow run ID mismatch; expected %s.\n' "$expected_run_id" >&2
    exit 1
  }
fi

printf 'Source commit: %s\n' "$source_sha"
[[ -z "$expected_run_id" ]] || printf 'Workflow run ID: %s\n' "$expected_run_id"
printf 'Artifact integrity verified. This does not prove installation or offline-device acceptance.\n'
