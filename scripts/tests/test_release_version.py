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
    def test_first_release_uses_the_source_version(self):
        self.assertEqual({"version": "1.0.0", "tag": "v1.0.0", "version_code": 1_000_000},
                         release.resolve_version("1.0.0", []))

    def test_old_mvp_tags_advance_to_one_point_zero(self):
        for bump in ("patch", "minor", "major"):
            self.assertEqual("1.0.0", release.resolve_version("1.0.0", ["v0.1.0", "v0.2.0"], bump)["version"])

    def test_patch_minor_and_major_bumps(self):
        for bump, expected in (("patch", "1.2.4"), ("minor", "1.3.0"), ("major", "2.0.0")):
            self.assertEqual(expected, release.resolve_version("1.0.0", ["v1.2.3"], bump)["version"])

    def test_versions_are_compared_numerically_and_nonstable_tags_are_ignored(self):
        tags = ["v1.9.9", "v1.10.0", "v2.0.0-rc.1", "notes", "3.0.0", "v01.2.3"]
        self.assertEqual("1.10.1", release.resolve_version("1.0.0", tags)["version"])

    def test_exact_version_overrides_the_bump(self):
        self.assertEqual("1.4.0", release.resolve_version("1.0.0", ["v1.2.3"], "major", " 1.4.0 ")["version"])

    def test_a_new_source_floor_does_not_override_a_larger_bump(self):
        self.assertEqual("2.0.0", release.resolve_version("1.2.0", ["v1.1.9"], "major")["version"])

    def test_existing_older_and_below_source_versions_are_rejected(self):
        for requested in ("1.2.3", "1.1.0", "0.2.0"):
            with self.subTest(requested=requested), self.assertRaises(ValueError):
                release.resolve_version("1.0.0", ["v1.2.3"], requested=requested)

    def test_invalid_versions_and_injected_output_are_rejected(self):
        for requested in ("v1.0.0", "01.0.0", "1.0", "1.0.0-beta.1", "-1.0.0", "1.0.0\ntag=other"):
            with self.subTest(requested=requested), self.assertRaises(ValueError):
                release.resolve_version("1.0.0", [], requested=requested)

    def test_component_limits_and_overflow_have_clear_errors(self):
        for version in ("2000.0.0", "1.1000.0", "1.0.1000"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                release.parse_version(version)
        with self.assertRaises(ValueError):
            release.resolve_version("1.0.0", ["v1.2.999"])
        self.assertEqual("1.3.0", release.resolve_version("1.0.0", ["v1.2.999"], "minor")["version"])
        self.assertLess(release.resolve_version("1999.999.999", [])["version_code"], 2_100_000_000)

    def test_version_codes_increase_across_semantic_versions(self):
        versions = ["1.0.0", "1.0.1", "1.0.999", "1.1.0", "1.999.999", "2.0.0"]
        codes = [release.resolve_version(version, [])["version_code"] for version in versions]
        self.assertEqual(sorted(set(codes)), codes)
        self.assertGreater(codes[0], 100_000)

    def test_cli_reads_real_tags_and_writes_actions_outputs(self):
        with tempfile.TemporaryDirectory() as folder:
            subprocess.run(["git", "init", "-q", folder], check=True)
            subprocess.run(["git", "-C", folder, "-c", "user.name=Release test", "-c", "user.email=test@example.invalid",
                            "-c", "commit.gpgsign=false", "commit", "--allow-empty", "-qm", "fixture"], check=True)
            for tag in ("v0.2.0", "v1.0.0"):
                subprocess.run(["git", "-C", folder, "tag", tag], check=True)
            (Path(folder) / "gradle.properties").write_text("nia.versionName=1.0.0\n")
            env = os.environ.copy()
            env.update(GITHUB_ACTIONS="true", GITHUB_REF="refs/heads/main", GITHUB_OUTPUT=folder + "/outputs",
                       RELEASE_BUMP="minor", RELEASE_VERSION="")
            result = subprocess.run([sys.executable, str(SCRIPT)], cwd=folder, env=env, capture_output=True, text=True, check=True)
            self.assertEqual("1.1.0", json.loads(result.stdout)["version"])
            self.assertEqual("version=1.1.0\ntag=v1.1.0\nversion_code=1001000\n", (Path(folder) / "outputs").read_text())

    def test_actions_rejects_builds_from_other_branches(self):
        env = os.environ.copy()
        env.update(GITHUB_ACTIONS="true", GITHUB_REF="refs/heads/feature")
        result = subprocess.run([sys.executable, str(SCRIPT)], env=env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("main branch", result.stderr)


if __name__ == "__main__":
    unittest.main()
