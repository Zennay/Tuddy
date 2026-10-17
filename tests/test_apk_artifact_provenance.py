"""Offline proof that a Tuddy debug artifact ships independently checkable receipts."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/android.yml"


def validate_artifact_contract(text: str) -> None:
    assert 'name: Tuddy-debug-apk' in text
    assert 'test -s "$out/app-debug.apk"' in text
    assert 'sha256sum app-debug.apk > app-debug.apk.sha256' in text
    assert 'sha256sum --check app-debug.apk.sha256' in text
    assert 'git rev-parse HEAD > "$out/source-commit.txt"' in text
    assert 'EXPECTED_SHA: ${{ github.sha }}' in text
    assert 'test "$(cat "$out/source-commit.txt")" = "$EXPECTED_SHA"' in text
    for leaf in ['app-debug.apk', 'app-debug.apk.sha256', 'source-commit.txt']:
        assert 'app/build/outputs/apk/debug/' + leaf in text
    assert 'if-no-files-found: error' in text
    assert 'retention-days: 14' in text


class ArtifactProvenanceTest(unittest.TestCase):
    def test_workflow_includes_reproducible_apk_and_sha_receipts(self):
        validate_artifact_contract(WORKFLOW.read_text(encoding='utf8'))

    def test_regressions_cannot_drop_proof_or_upload_only_the_apk(self):
        source = WORKFLOW.read_text(encoding='utf8')
        for changed in [
            source.replace('sha256sum --check app-debug.apk.sha256', 'echo unchecked'),
            source.replace('git rev-parse HEAD > "$out/source-commit.txt"', 'echo unknown > "$out/source-commit.txt"'),
            source.replace('app/build/outputs/apk/debug/source-commit.txt', 'some/untracked/file'),
            source.replace('app/build/outputs/apk/debug/app-debug.apk.sha256', 'some/untracked/file'),
            source.replace('EXPECTED_SHA: ${{ github.sha }}', 'EXPECTED_SHA: main'),
        ]:
            self.assertNotEqual(changed, source)
            with self.assertRaises(AssertionError):
                validate_artifact_contract(changed)

    def test_downloaded_sha256_receipt_detects_modified_apk(self):
        with tempfile.TemporaryDirectory(prefix='tuddy-artifact-proof-') as directory:
            work = Path(directory)
            apk = work / 'app-debug.apk'
            apk.write_bytes(b'synthetic-offline-debug-package')
            with (work / 'app-debug.apk.sha256').open('w', encoding='utf8') as receipt:
                subprocess.run(['sha256sum', apk.name], cwd=work, stdout=receipt, check=True)
            good = subprocess.run(['sha256sum', '--check', 'app-debug.apk.sha256'],
                                  cwd=work, capture_output=True, text=True)
            self.assertEqual(good.returncode, 0, good.stderr)
            apk.write_bytes(b'tampered')
            bad = subprocess.run(['sha256sum', '--check', 'app-debug.apk.sha256'],
                                 cwd=work, capture_output=True, text=True)
            self.assertNotEqual(bad.returncode, 0)


if __name__ == '__main__':
    unittest.main()
