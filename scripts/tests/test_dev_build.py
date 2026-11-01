from contextlib import contextmanager
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import dev_build as dev


class DevBuildTests(unittest.TestCase):
    def environment(self):
        return dict(GITHUB_REPOSITORY="robin-siegl/Nia", GITHUB_EVENT_NAME="workflow_dispatch",
                    GITHUB_REF="refs/heads/test-feature", GITHUB_SHA="a" * 40,
                    GITHUB_RUN_NUMBER="42", GITHUB_RUN_ATTEMPT="1", GITHUB_RUN_ID="12345")

    def metadata(self):
        return dev.build_metadata("1.5.0", "1005000", self.environment())

    @contextmanager
    def repository(self, properties="nia.versionName=1.5.0\nnia.versionCode=1005000\n"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            subprocess.run(["git", "init", "-q", directory], check=True)
            (root / "gradle.properties").write_text(properties)
            subprocess.run(["git", "-C", directory, "add", "."], check=True)
            subprocess.run(["git", "-C", directory, "-c", "user.name=Dev test", "-c", "user.email=test@example.invalid",
                            "-c", "commit.gpgsign=false", "commit", "-qm", "fixture"], check=True)
            subprocess.run(["git", "-C", directory, "tag", "v1.5.0"], check=True)
            env = os.environ.copy()
            env.update(self.environment(), GITHUB_SHA=subprocess.check_output(["git", "-C", directory, "rev-parse", "HEAD"], text=True).strip(),
                       GITHUB_OUTPUT=str(root / "outputs"), GITHUB_STEP_SUMMARY=str(root / "summary"))
            yield root, env

    def test_runs_and_retries_are_identifiable_without_changing_the_base_version_or_code(self):
        original = self.metadata()
        self.assertEqual("1.5.0-dev.42.1+aaaaaaa", original["version_name"])
        self.assertEqual("nia-1.5.0-dev-42.1-aaaaaaa.apk", original["apk_name"])
        self.assertEqual("dev.robinsiegl.nia.dev", original["application_id"])
        for key in ("GITHUB_RUN_NUMBER", "GITHUB_RUN_ATTEMPT"):
            env = self.environment(); env[key] = "43"
            updated = dev.build_metadata("1.5.0", "1005000", env)
            self.assertNotEqual(original["version_name"], updated["version_name"])
            self.assertNotEqual(original["artifact_name"], updated["artifact_name"])
            self.assertEqual(("1.5.0", 1005000), (updated["version"], updated["version_code"]))

    def test_manual_own_repository_and_branch_context_is_required(self):
        changes = [("GITHUB_REPOSITORY", "someone/fork"), ("GITHUB_EVENT_NAME", "pull_request"),
                   ("GITHUB_EVENT_NAME", "push"), ("GITHUB_REF", "refs/tags/v1.5.0"), ("GITHUB_REF", "refs/heads/"),
                   ("GITHUB_REF", "refs/heads/example\nother=value"), ("GITHUB_SHA", "aaaaaaa"),
                   ("GITHUB_SHA", "a" * 39 + "\n")]
        for key in ("GITHUB_RUN_NUMBER", "GITHUB_RUN_ATTEMPT", "GITHUB_RUN_ID"):
            changes.extend((key, invalid) for invalid in ("", "0", "-1", "01", "42\nsecret=value", "42;exit"))
        for key, invalid in changes:
            env = self.environment(); env[key] = invalid
            with self.subTest(key=key, invalid=invalid), self.assertRaises(ValueError):
                dev.build_metadata("1.5.0", "1005000", env)

    def test_a_released_version_can_be_rebuilt_from_a_feature_branch_without_editing_source_or_tags(self):
        with self.repository() as (root, env):
            before = (root / "gradle.properties").read_bytes()
            for attempt in ("1", "2"):
                env["GITHUB_RUN_ATTEMPT"] = attempt
                result = subprocess.run([sys.executable, str(SCRIPTS / "dev_build.py"), "metadata"],
                                        cwd=root, env=env, text=True, capture_output=True)
                self.assertEqual(0, result.returncode, result.stderr)
                info = json.loads(result.stdout)
                self.assertEqual(1005000, info["version_code"])
                self.assertIn(f"-dev.42.{attempt}+", info["version_name"])
            self.assertEqual(before, (root / "gradle.properties").read_bytes())
            self.assertEqual("v1.5.0", subprocess.check_output(["git", "-C", str(root), "tag", "--list"], text=True).strip())
            outputs = (root / "outputs").read_text()
            self.assertNotIn("tag=", outputs)
            self.assertIn("build_id=42.2", outputs)

    def test_invalid_project_values_fail_before_writing_outputs(self):
        for version, code in (("1.5.0", "1004000"), ("1.5.0-dev", "1005000"), ("01.5.0", "1005000")):
            with self.subTest(version=version), self.repository(f"nia.versionName={version}\nnia.versionCode={code}\n") as (root, env):
                result = subprocess.run([sys.executable, str(SCRIPTS / "dev_build.py"), "metadata"],
                                        cwd=root, env=env, text=True, capture_output=True)
                self.assertNotEqual(0, result.returncode)
                self.assertFalse((root / "outputs").exists())

    def test_wrong_checkout_fails_before_writing_outputs(self):
        with self.repository() as (root, env):
            env["GITHUB_SHA"] = "b" * 40
            result = subprocess.run([sys.executable, str(SCRIPTS / "dev_build.py"), "metadata"],
                                    cwd=root, env=env, text=True, capture_output=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("checked-out commit", result.stderr)
            self.assertFalse((root / "outputs").exists())

    def test_only_the_requested_optimized_dev_apk_can_be_packaged(self):
        valid = ("package: name='dev.robinsiegl.nia.dev' versionCode='1005000' "
                 "versionName='1.5.0-dev.42.1+aaaaaaa' platformBuildVersionCode='36'\n"
                 "application-label:'nia Dev'\n")
        dev.validate_apk_identity(valid, self.metadata())
        invalid = [valid.replace("dev.robinsiegl.nia.dev", "dev.robinsiegl.nia"),
                   valid.replace("dev.robinsiegl.nia.dev", "dev.robinsiegl.nia.debug"),
                   valid.replace("1005000", "1004000"), valid.replace("42.1+aaaaaaa", "42.2+aaaaaaa"),
                   valid.replace("'nia Dev'", "'nia'"), valid + "application-debuggable\n"]
        for badging in invalid:
            with self.subTest(badging=badging), self.assertRaises(ValueError):
                dev.validate_apk_identity(badging, self.metadata())

    def test_download_contains_apk_checksum_and_source_identity_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); apk = root / "input.apk"; apk.write_bytes(b"verified APK fixture")
            info = self.metadata()
            target = dev.prepare_download(apk, root / "download", info, "b" * 64)
            self.assertEqual(apk.read_bytes(), target.read_bytes())
            self.assertEqual({info["apk_name"], "BUILD_INFO.json", "SHA256SUMS.txt"}, {p.name for p in target.parent.iterdir()})
            expected = hashlib.sha256(apk.read_bytes()).hexdigest()
            self.assertEqual(f"{expected}  {target.name}\n", (target.parent / "SHA256SUMS.txt").read_text())
            saved = json.loads((target.parent / "BUILD_INFO.json").read_text())
            self.assertEqual(expected, saved["apk_sha256"])
            self.assertEqual("b" * 64, saved["signer_sha256"])
            self.assertEqual(info["commit"], saved["commit"])
            self.assertEqual("refs/heads/test-feature", saved["source_ref"])
            self.assertNotIn("apk_sha256", info)

    def test_a_stale_download_directory_cannot_leak_old_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); output = root / "download"; output.mkdir()
            (output / "old.apk").write_bytes(b"old")
            apk = root / "input.apk"; apk.write_bytes(b"new")
            with self.assertRaises(FileExistsError):
                dev.prepare_download(apk, output, self.metadata(), "b" * 64)
            self.assertEqual(["old.apk"], [p.name for p in output.iterdir()])

    def test_summary_links_to_this_runs_artifact_and_explains_installation(self):
        with tempfile.TemporaryDirectory() as directory:
            summary = Path(directory) / "summary"
            with mock.patch.dict(os.environ, GITHUB_STEP_SUMMARY=str(summary)):
                dev.write_summary(self.metadata(), self.metadata()["run_url"] + "/artifacts/6789")
            text = summary.read_text()
            for value in ("Download the APK bundle", "/artifacts/6789", "30 days", "GitHub sign-in", "separate settings", "BUILD_INFO.json"):
                self.assertIn(value, text)

    def test_foreign_or_injected_artifact_links_are_rejected(self):
        for url in ("https://example.com/apk", self.metadata()["run_url"] + "/artifacts/1\nother",
                    "https://github.com/robin-siegl/Nia/actions/runs/999/artifacts/1"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                dev.write_summary(self.metadata(), url)


if __name__ == "__main__":
    unittest.main()
