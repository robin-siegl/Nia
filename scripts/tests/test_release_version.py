from contextlib import contextmanager
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "release_version.py"
spec = importlib.util.spec_from_file_location("nia_release_version", SCRIPT)
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseVersionTests(unittest.TestCase):
    @contextmanager
    def repository(self, version="1.2.0", code="1002000", tags=("v0.2.0", "v1.1.0")):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            subprocess.run(["git", "init", "-q", folder], check=True)
            subprocess.run(["git", "-C", folder, "-c", "user.name=Release test", "-c", "user.email=test@example.invalid",
                            "-c", "commit.gpgsign=false", "commit", "--allow-empty", "-qm", "fixture"], check=True)
            for tag in tags:
                subprocess.run(["git", "-C", folder, "tag", tag], check=True)
            (root / "gradle.properties").write_text(f"nia.versionName={version}\nnia.versionCode={code}\n")
            env = os.environ.copy()
            env.update(GITHUB_ACTIONS="true", GITHUB_REF="refs/heads/main", GITHUB_OUTPUT=str(root / "outputs"),
                       GITHUB_STEP_SUMMARY=str(root / "summary"), RELEASE_BUMP="", RELEASE_VERSION="")
            yield root, env

    def cli(self, root, env, *args):
        return subprocess.run([sys.executable, str(SCRIPT), *args], cwd=root, env=env, capture_output=True, text=True)

    def test_first_release_uses_both_project_values(self):
        self.assertEqual({"version": "1.0.0", "tag": "v1.0.0", "version_code": 1_000_000},
                         release.resolve_version("1.0.0", "1000000", []))

    def test_existing_tags_never_choose_or_bump_the_project_version(self):
        for version, code in (("1.1.1", 1_001_001), ("1.2.0", 1_002_000), ("2.0.0", 2_000_000)):
            with self.subTest(version=version):
                result = release.resolve_version(version, code, ["v0.2.0", "v1.0.0", "v1.1.0"])
                self.assertEqual(version, result["version"])
                self.assertEqual(code, result["version_code"])

    def test_versions_are_compared_numerically_and_nonstable_tags_are_ignored(self):
        tags = ["v1.9.9", "v1.10.0", "v2.0.0-rc.1", "notes", "3.0.0", "v01.2.3"]
        self.assertEqual("1.10.1", release.resolve_version("1.10.1", 1_010_001, tags)["version"])
        with self.assertRaises(ValueError):
            release.resolve_version("1.9.10", 1_009_010, tags)

    def test_duplicate_and_older_versions_require_a_committed_version_change(self):
        for version, code in (("1.2.3", 1_002_003), ("1.1.0", 1_001_000), ("0.2.0", 2_000)):
            with self.subTest(version=version), self.assertRaisesRegex(ValueError, "gradle.properties"):
                release.resolve_version(version, code, ["v1.2.3"])

    def test_invalid_versions_and_injected_output_are_rejected(self):
        for version in ("v1.0.0", "01.0.0", "1.0", "1.0.0-beta.1", "-1.0.0", "1.0.0\ntag=other"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                release.resolve_version(version, 1_000_000, [])

    def test_component_limits_and_positive_android_codes(self):
        for version in ("2000.0.0", "1.1000.0", "1.0.1000", "0.0.0"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                release.resolve_version(version, 1, [])
        self.assertEqual(1_999_999_999, release.resolve_version("1999.999.999", "1999999999", [])["version_code"])

    def test_mismatched_or_malformed_version_codes_are_rejected(self):
        for code in ("1000000", "0", "-1", "01002000", "1002000\ntag=other", "not-a-code", "1002000.0"):
            with self.subTest(code=code), self.assertRaisesRegex(ValueError, "nia.versionCode must be 1002000"):
                release.resolve_version("1.2.0", code, [])

    def test_version_codes_increase_across_semantic_versions(self):
        versions = [("1.0.0", 1_000_000), ("1.0.1", 1_000_001), ("1.0.999", 1_000_999),
                    ("1.1.0", 1_001_000), ("1.999.999", 1_999_999), ("2.0.0", 2_000_000)]
        codes = [release.resolve_version(version, code, [])["version_code"] for version, code in versions]
        self.assertEqual(sorted(set(codes)), codes)
        self.assertGreater(codes[0], 100_000)

    def test_properties_allow_comments_and_spaces(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "gradle.properties"
            path.write_text("# nia.versionName=9.0.0\n ! nia.versionCode=9000000\norg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8\n"
                            " nia.versionName = 1.2.0 \n nia.versionCode = 1002000 \n")
            self.assertEqual(("1.2.0", "1002000"), release.read_project_version(path))

    def test_missing_and_duplicate_properties_have_actionable_errors(self):
        contents = [("nia.versionName=1.2.0\n", "Set nia.versionCode"),
                    ("nia.versionCode=1002000\n", "Set nia.versionName"),
                    ("nia.versionName=1.2.0\nnia.versionName=2.0.0\nnia.versionCode=1002000\n", "only once"),
                    ("nia.versionName=1.2.0\nnia.versionCode=1002000\nnia.versionCode=2000000\n", "only once")]
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "gradle.properties"
            for text, message in contents:
                with self.subTest(text=text), self.assertRaisesRegex(ValueError, message):
                    path.write_text(text)
                    release.read_project_version(path)

    def test_cli_reads_project_values_and_writes_actions_outputs_without_changing_source(self):
        with self.repository() as (root, env):
            original = (root / "gradle.properties").read_bytes()
            result = self.cli(root, env)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual({"version": "1.2.0", "tag": "v1.2.0", "version_code": 1_002_000}, json.loads(result.stdout))
            self.assertEqual("version=1.2.0\ntag=v1.2.0\nversion_code=1002000\n", (root / "outputs").read_text())
            self.assertIn("nia 1.2.0", (root / "summary").read_text())
            self.assertEqual(original, (root / "gradle.properties").read_bytes())

    def test_removed_environment_overrides_cannot_change_the_release(self):
        with self.repository() as (root, env):
            env.update(RELEASE_BUMP="major", RELEASE_VERSION="9.9.9")
            result = self.cli(root, env)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual("1.2.0", json.loads(result.stdout)["version"])

    def test_removed_command_line_version_overrides_are_rejected(self):
        with self.repository() as (root, env):
            for args in (("--version", "9.9.9"), ("--bump", "major")):
                with self.subTest(args=args):
                    result = self.cli(root, env, *args)
                    self.assertNotEqual(0, result.returncode)
                    self.assertIn("unrecognized arguments", result.stderr)
                    self.assertFalse((root / "outputs").exists())

    def test_cli_rejects_an_existing_tag_before_writing_release_outputs(self):
        with self.repository(tags=("v1.2.0",)) as (root, env):
            result = self.cli(root, env)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("Existing tags are never replaced", result.stderr)
            self.assertFalse((root / "outputs").exists())

    def test_cli_rejects_mismatched_codes_before_writing_release_outputs(self):
        with self.repository(code="1000000") as (root, env):
            result = self.cli(root, env)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("nia.versionCode must be 1002000", result.stderr)
            self.assertFalse((root / "outputs").exists())

    def test_actions_rejects_builds_from_other_branches(self):
        with self.repository() as (root, env):
            env["GITHUB_REF"] = "refs/heads/feature"
            result = self.cli(root, env)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("main branch", result.stderr)


if __name__ == "__main__":
    unittest.main()
