import base64
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

SCRIPTS = Path(__file__).resolve().parents[1]
SECRET_NAMES = (
    "ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD",
)


class SigningTests(unittest.TestCase):
    def run_prepare(self, folder, values):
        env = {k: v for k, v in os.environ.items() if k not in SECRET_NAMES}
        env.update(values, RUNNER_TEMP=folder, GITHUB_ENV=folder + "/env")
        return subprocess.run([sys.executable, str(SCRIPTS / "prepare_signing.py")],
                              env=env, capture_output=True, text=True)

    def example_secrets(self):
        return {
            "ANDROID_KEYSTORE_BASE64": base64.b64encode(b"keystore fixture" * 20).decode(),
            "ANDROID_KEYSTORE_PASSWORD": "store password % with $ and =",
            "ANDROID_KEY_ALIAS": "nia release",
            "ANDROID_KEY_PASSWORD": "different key password",
        }

    @unittest.skipUnless(shutil.which("keytool"), "JDK keytool is required")
    def test_setup_reuses_the_existing_release_key(self):
        with tempfile.TemporaryDirectory() as folder:
            script = Path(folder) / "scripts/setup_signing.py"
            script.parent.mkdir()
            shutil.copy2(SCRIPTS / "setup_signing.py", script)
            subprocess.run([sys.executable, str(script)], capture_output=True, check=True)
            bundle = Path(folder) / ".signing/nia-signing.json"
            original = bundle.read_bytes()
            subprocess.run([sys.executable, str(script)], capture_output=True, check=True)
            self.assertEqual(original, bundle.read_bytes())
            value = json.loads(original)
            self.assertGreaterEqual(len(value["storePassword"]), 40)

    def test_missing_secret_has_an_actionable_error(self):
        with tempfile.TemporaryDirectory() as folder:
            result = self.run_prepare(folder, {})
            self.assertNotEqual(0, result.returncode)
            for name in SECRET_NAMES:
                self.assertIn(name, result.stderr)
            self.assertIn("docs/RELEASING.md", result.stderr)

    def test_invalid_secret_does_not_write_a_key(self):
        for encoded in ("not valid base64!", "YQ=="):
            with self.subTest(encoded=encoded), tempfile.TemporaryDirectory() as folder:
                values = self.example_secrets()
                values["ANDROID_KEYSTORE_BASE64"] = encoded
                result = self.run_prepare(folder, values)
                self.assertNotEqual(0, result.returncode)
                self.assertIn("ANDROID_KEYSTORE_BASE64", result.stderr)
                self.assertFalse((Path(folder) / "nia-release.p12").exists())
                self.assertFalse((Path(folder) / "env").exists())

    def test_four_secrets_restore_exact_keystore_and_credentials(self):
        with tempfile.TemporaryDirectory() as folder:
            values = self.example_secrets()
            result = self.run_prepare(folder, values)
            self.assertEqual(0, result.returncode, result.stderr)
            key = Path(folder) / "nia-release.p12"
            self.assertEqual(b"keystore fixture" * 20, key.read_bytes())
            self.assertEqual(
                "NIA_KEYSTORE_PATH=" + str(key) + "\n"
                "NIA_STORE_PASSWORD=" + values["ANDROID_KEYSTORE_PASSWORD"] + "\n"
                "NIA_KEY_ALIAS=" + values["ANDROID_KEY_ALIAS"] + "\n"
                "NIA_KEY_PASSWORD=" + values["ANDROID_KEY_PASSWORD"] + "\n",
                (Path(folder) / "env").read_text(),
            )
            if os.name != "nt":
                self.assertEqual(0o600, key.stat().st_mode & 0o777)
            self.assertIn("::add-mask::store password %25 with $ and =", result.stdout)

    def test_wrapped_base64_is_accepted(self):
        with tempfile.TemporaryDirectory() as folder:
            values = self.example_secrets()
            encoded = values["ANDROID_KEYSTORE_BASE64"]
            values["ANDROID_KEYSTORE_BASE64"] = encoded[:64] + "\r\n" + encoded[64:] + "\n"
            result = self.run_prepare(folder, values)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual(b"keystore fixture" * 20, (Path(folder) / "nia-release.p12").read_bytes())

    def test_multiline_credentials_cannot_inject_environment_variables(self):
        for name in SECRET_NAMES[1:]:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as folder:
                values = self.example_secrets()
                values[name] += "\nOTHER=value"
                result = self.run_prepare(folder, values)
                self.assertNotEqual(0, result.returncode)
                self.assertIn(name, result.stderr)
                self.assertFalse((Path(folder) / "env").exists())

    def test_helper_uploads_the_same_four_repository_secrets(self):
        with tempfile.TemporaryDirectory() as folder:
            signing = Path(folder) / ".signing"
            signing.mkdir()
            bundle = {"keystoreBase64": "encoded fixture", "storePassword": "store fixture",
                      "keyAlias": "nia", "keyPassword": "key fixture"}
            (signing / "nia-signing.json").write_text(json.dumps(bundle))
            spec = importlib.util.spec_from_file_location("nia_signing_setup", SCRIPTS / "setup_signing.py")
            setup = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(setup)
            with mock.patch.object(setup, "__file__", str(Path(folder) / "scripts/setup_signing.py")), \
                    mock.patch.object(sys, "argv", ["setup_signing.py", "--upload"]), \
                    mock.patch.object(setup.shutil, "which", return_value="gh"), \
                    mock.patch.object(setup.subprocess, "run") as run, \
                    mock.patch("builtins.print"):
                setup.main()
            expected_values = [bundle[field] for field in ("keystoreBase64", "storePassword", "keyAlias", "keyPassword")]
            self.assertEqual([
                mock.call(["gh", "secret", "set", name, "--repo", "robin-siegl/Nia"],
                          input=value.encode(), check=True)
                for name, value in zip(SECRET_NAMES, expected_values)
            ], run.call_args_list)


if __name__ == "__main__":
    unittest.main()
