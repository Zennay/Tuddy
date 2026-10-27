#!/usr/bin/env bash
# Resolve and verify the CURRENT Tuddy main-push APK artifact.
# Requires a trusted, already-downloaded GitHub artifact ZIP and GitHub CLI auth.
# Artifact integrity is NOT physical Android 12+ offline acceptance.
set -euo pipefail

usage() {
  printf 'Usage: bash scripts/verify-current-tuddy-main-apk.sh DOWNLOADED_ARTIFACT.zip\n' >&2
  exit 2
}

die() {
  printf 'Tuddy APK handoff: %s\n' "$1" >&2
  exit 1
}

[[ $# -eq 1 ]] || usage
for cmd in gh unzip sha256sum awk wc realpath tr bash; do
  command -v "$cmd" >/dev/null 2>&1 || die "Missing required command: $cmd"
done

artifact_zip=$(realpath -- "$1")
[[ -f "$artifact_zip" ]] || die "ZIP not found: $artifact_zip"
repo=Zennay/Tuddy

# A successful workflow on an older commit is never a current device candidate.
main_sha=$(gh api "repos/$repo/commits/main" --jq '.sha') ||
  die 'Could not resolve the current GitHub main commit.'
[[ "$main_sha" =~ ^[0-9a-f]{40}$ ]] || die 'Unexpected main commit identity.'
run_id=$(gh api "repos/$repo/actions/runs?branch=main&event=push&status=success&per_page=100" \
  --jq ".workflow_runs | map(select(.head_sha == \"$main_sha\" and .name == \"Android APK (self-hosted)\" and .event == \"push\" and .head_branch == \"main\" and .conclusion == \"success\")) | .[0].id // empty") ||
  die 'Could not inspect successful main-push runs.'
[[ "$run_id" =~ ^[0-9]+$ ]] ||
  die 'No successful self-hosted Android main-push workflow exists for the CURRENT main SHA. Wait for CI; do not install a previous APK.'

# The GitHub artifact digest authenticates the complete downloaded ZIP.
artifact_meta=$(gh api "repos/$repo/actions/runs/$run_id/artifacts" \
  --jq '[.artifacts[] | select(.name == "Tuddy-debug-apk" and .expired == false)] | if length == 1 then .[0] | [.id, .digest, .size_in_bytes] | @tsv else empty end') ||
  die 'Could not inspect the chosen workflow artifact.'
[[ -n "$artifact_meta" ]] || die 'Exactly one unexpired Tuddy-debug-apk artifact is required.'
IFS=$'\t' read -r artifact_id artifact_digest expected_zip_bytes <<< "$artifact_meta"
[[ "$artifact_id" =~ ^[0-9]+$ ]] || die 'Unexpected artifact ID.'
[[ "$artifact_digest" =~ ^sha256:[0-9a-f]{64}$ ]] ||
  die 'GitHub did not provide an authenticated ZIP SHA-256 digest.'
[[ "$expected_zip_bytes" =~ ^[0-9]+$ ]] || die 'Invalid ZIP size from GitHub.'

actual_zip_bytes=$(wc -c < "$artifact_zip" | tr -d '[:space:]')
[[ "$actual_zip_bytes" == "$expected_zip_bytes" ]] ||
  die "Downloaded ZIP byte count differs from artifact $artifact_id."

# Read a single small checksum sidecar from the ZIP; the existing verifier
# subsequently checks that checksum against the actual extracted APK.
checksum_entry=$(unzip -Z1 "$artifact_zip" |
  awk '/(^|\/)app-debug\.apk\.sha256$/ { print }') ||
  die 'Cannot read artifact ZIP entries.'
[[ -n "$checksum_entry" && "$checksum_entry" != *$'\n'* ]] ||
  die 'Expected exactly one app-debug.apk.sha256 entry.'
checksum_text=$(unzip -p "$artifact_zip" "$checksum_entry") ||
  die 'Cannot read the APK checksum sidecar.'
[[ "$checksum_text" =~ ^[0-9a-f]{64}[[:space:]][[:space:]]app-debug[.]apk$ ]] ||
  die 'APK checksum sidecar must contain one exact sha256sum line.'
apk_sha=$(printf '%s' "$checksum_text" | awk '{ print $1 }')

zip_sha=$(printf '%s' "$artifact_digest" | awk -F: '{ print $2 }')
script_dir=$(cd -- "$(dirname -- "$0")" && pwd)
bash "$script_dir/verify-tuddy-apk-artifact.sh" \
  "$artifact_zip" "$zip_sha" "$apk_sha" "$main_sha" "$run_id" ||
  die 'Artifact ZIP, extracted APK, source SHA or workflow provenance failed verification.'

# A concurrent merge invalidates the candidate, even if all byte hashes pass.
latest_main_sha=$(gh api "repos/$repo/commits/main" --jq '.sha') ||
  die 'Cannot recheck main after verification.'
[[ "$latest_main_sha" == "$main_sha" ]] ||
  die 'main advanced during verification. Download and verify the newer main artifact.'
printf '\nVerified CURRENT main handoff: %s\n' "$main_sha"
printf 'Workflow: https://github.com/%s/actions/runs/%s\n' "$repo" "$run_id"
printf 'Artifact: https://github.com/%s/actions/runs/%s/artifacts/%s\n' "$repo" "$run_id" "$artifact_id"
printf 'APK SHA-256: %s\n' "$apk_sha"
printf 'Physical installation, airplane-mode speech trials and human review: STILL REQUIRED.\n'