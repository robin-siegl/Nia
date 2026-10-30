#!/usr/bin/env python3
"""Create a persistent private release key and optionally configure the GitHub secret."""
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
    parser.add_argument("--upload", action="store_true", help="Set NIA_SIGNING_BUNDLE using an authenticated GitHub CLI")
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
    if args.upload:
        if not shutil.which("gh"):
            raise SystemExit("GitHub CLI is not installed. Add the JSON file below as the NIA_SIGNING_BUNDLE repository Actions secret.\n" + str(bundle_path))
        subprocess.run(["gh", "secret", "set", "NIA_SIGNING_BUNDLE", "--repo", args.repo],
                       input=bundle_path.read_bytes(), check=True)
        print("NIA_SIGNING_BUNDLE configured. Run the Production APK workflow on GitHub.")
    else:
        print("Add the complete contents of this file as the NIA_SIGNING_BUNDLE repository Actions secret:")
        print(bundle_path)
        print("Or run this script again with --upload after signing into GitHub CLI with gh auth login.")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError:
        sys.exit("Signing setup failed. Your existing signing key has not been replaced.")
