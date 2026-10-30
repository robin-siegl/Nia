#!/usr/bin/env python3
"""Create a persistent private release key and optionally configure GitHub secrets."""
import argparse
import base64
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys


def find_keytool():
    found = shutil.which("keytool")
    candidates = [found] if found else []
    if os.environ.get("JAVA_HOME"):
        candidates.append(str(Path(os.environ["JAVA_HOME"]) / "bin" / ("keytool.exe" if os.name == "nt" else "keytool")))
    if os.name == "nt":
        candidates += [str(Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Android/Android Studio/jbr/bin/keytool.exe")]
    else:
        candidates += ["/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"]
    return next((p for p in candidates if Path(p).is_file()), None)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--upload", action="store_true", help="Set the four ANDROID_* secrets using an authenticated GitHub CLI")
    parser.add_argument("--repo", default="robin-siegl/Nia")
    args = parser.parse_args()
    folder = Path(__file__).resolve().parent.parent / ".signing"
    folder.mkdir(mode=0o700, exist_ok=True)
    bundle_path = folder / "nia-signing.json"
    key_path = folder / "nia-release.p12"
    if not bundle_path.exists():
        if key_path.exists():
            raise SystemExit("An existing key was found without its bundle. Restore its bundle instead of replacing the key.")
        keytool = find_keytool()
        if not keytool:
            raise SystemExit("Install JDK 17+ or Android Studio, then rerun this script. keytool is included in both.")
        password = secrets.token_urlsafe(36)
        env = os.environ.copy()
        env["NIA_SETUP_PASSWORD"] = password
        subprocess.run([keytool, "-genkeypair", "-keystore", str(key_path), "-storetype", "PKCS12",
                        "-alias", "nia", "-keyalg", "RSA", "-keysize", "3072", "-validity", "10000",
                        "-dname", "CN=nia release", "-storepass:env", "NIA_SETUP_PASSWORD",
                        "-keypass:env", "NIA_SETUP_PASSWORD", "-noprompt"], env=env, check=True)
        bundle = {"keystoreBase64": base64.b64encode(key_path.read_bytes()).decode(),
                  "storePassword": password, "keyAlias": "nia", "keyPassword": password}
        bundle_path.write_text(json.dumps(bundle), encoding="utf-8")
        bundle_path.chmod(0o600)
        key_path.chmod(0o600)
        print("Created the release signing key. Keep a private backup of the .signing directory.")
    else:
        print("Reusing the existing signing bundle; no new key was generated.")
    bundle = json.loads(bundle_path.read_text(encoding="utf-8"))
    fields = {
        "ANDROID_KEYSTORE_BASE64": "keystoreBase64",
        "ANDROID_KEYSTORE_PASSWORD": "storePassword",
        "ANDROID_KEY_ALIAS": "keyAlias",
        "ANDROID_KEY_PASSWORD": "keyPassword",
    }
    if args.upload:
        if not shutil.which("gh"):
            raise SystemExit("GitHub CLI is not installed. Add the four ANDROID_* secrets manually using docs/RELEASING.md. Local backup:\n" + str(bundle_path))
        for secret_name, field in fields.items():
            subprocess.run(["gh", "secret", "set", secret_name, "--repo", args.repo],
                           input=bundle[field].encode("utf-8"), check=True)
        print("The four ANDROID_* signing secrets are configured. Run the Production APK workflow on GitHub.")
    else:
        print("Your private signing backup is saved at:")
        print(bundle_path)
        print("To configure repository Actions secrets manually, copy each JSON field's value separately:")
        for secret_name, field in fields.items():
            print(secret_name + " <- " + field)
        print("Or run this script again with --upload after signing into GitHub CLI with gh auth login.")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError:
        sys.exit("Signing setup failed. Your existing signing key has not been replaced.")
