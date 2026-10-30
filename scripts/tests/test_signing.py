import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]


class SigningTests(unittest.TestCase):
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
        env = {k: v for k, v in os.environ.items() if k != "NIA_SIGNING_BUNDLE"}
        result = subprocess.run([sys.executable, str(SCRIPTS / "prepare_signing.py")], env=env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("NIA_SIGNING_BUNDLE", result.stderr)

    def test_invalid_secret_does_not_write_a_key(self):
        with tempfile.TemporaryDirectory() as folder:
            env = os.environ.copy()
            env.update(NIA_SIGNING_BUNDLE='{"invalid":true}', RUNNER_TEMP=folder, GITHUB_ENV=folder + "/env")
            result = subprocess.run([sys.executable, str(SCRIPTS / "prepare_signing.py")], env=env, capture_output=True)
            self.assertNotEqual(0, result.returncode)
            self.assertFalse((Path(folder) / "nia-release.p12").exists())


if __name__ == "__main__":
    unittest.main()
