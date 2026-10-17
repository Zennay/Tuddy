"""Offline, mutation-tested assurance for source/proposed-merge Android CI identity."""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/android.yml"
CHECKOUT = "actions/checkout@11d5960a326750d5838078e36cf38b85af677262"


def check_workflow(source: str) -> None:
    assert re.search(r"(?m)^permissions:\s*\n  contents: read$", source)
    assert "pull_request_target:" not in source
    assert "secrets." not in source
    assert "write-all" not in source
    assert source.count("uses: " + CHECKOUT) == 2, "both checkouts must be immutable"
    assert not re.search(r"uses:\s*actions/checkout@(?!11d5960a326750d5838078e36cf38b85af677262)", source)
    build = source.split("\n  build:\n", 1)[1].split("\n  source-head:\n", 1)[0]
    source_head = source.split("\n  source-head:\n", 1)[1]
    for job, revision in (
        (build, "${{ github.sha }}"),
        (source_head, "${{ github.event.pull_request.head.sha }}"),
    ):
        assert "uses: " + CHECKOUT in job
        with_checkout = job.split("uses: " + CHECKOUT, 1)[1].split("\n      - ", 1)[0]
        assert "persist-credentials: false" in with_checkout
        assert "ref: " + revision in with_checkout
        identity_step = (
            "Verify proposed-merge checkout identity without credentials"
            if job is build else "Verify exact immutable contributor revision"
        )
        assert identity_step in job
        verify = job.split(identity_step, 1)[1].split("\n      - ", 1)[0]
        # Do not accept a correct expected SHA in a *different* build step
        # when this checkout's own verification was weakened.
        assert "test \"$(git rev-parse HEAD)\" = \"$EXPECTED_SHA\"" in verify
        assert "test -z \"$(git status --porcelain)\"" in verify
        assert "EXPECTED_SHA: " + revision in verify
    assert "gradle :app:testDebugUnitTest :app:assembleDebug" in build
    assert "gradle :app:testDebugUnitTest" in source_head


class AndroidCIIdentityTest(unittest.TestCase):
    def test_committed_workflow_checks_immutable_identities_without_credentials(self):
        check_workflow(WORKFLOW.read_text(encoding="utf-8"))

    def test_ref_and_token_regressions_are_rejected(self):
        original = WORKFLOW.read_text(encoding="utf-8")
        cases = [
            original.replace("persist-credentials: false", "persist-credentials: true", 1),
            original.replace("ref: ${{ github.sha }}", "ref: main", 1),
            original.replace("EXPECTED_SHA: ${{ github.sha }}", "EXPECTED_SHA: ${{ github.ref }}", 1),
            original.replace(CHECKOUT, "actions/checkout@v4", 1),
            original.replace("test \"$(git rev-parse HEAD)\" = \"$EXPECTED_SHA\"", "echo unchecked", 1),
            original.replace("contents: read", "contents: write", 1),
        ]
        for changed in cases:
            self.assertNotEqual(changed, original)
            with self.subTest(change=changed[:125]):
                with self.assertRaises(AssertionError):
                    check_workflow(changed)


if __name__ == "__main__":
    unittest.main()
