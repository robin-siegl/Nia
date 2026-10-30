#!/usr/bin/env python3
"""Decode the repository Actions secret without exposing signing credentials in logs."""
import base64
import json
import os
from pathlib import Path
import re
import sys

raw = os.environ.get("NIA_SIGNING_BUNDLE", "")
if not raw:
    sys.exit("Add the NIA_SIGNING_BUNDLE repository Actions secret first. See docs/RELEASING.md or run python scripts/setup_signing.py --upload locally.")
try:
    bundle = json.loads(raw)
    for key in ["keystoreBase64", "storePassword", "keyAlias", "keyPassword"]:
        value = bundle[key]
        if not isinstance(value, str) or not value or any(c in value for c in "\r\n"):
            raise ValueError("Invalid signing field")
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", bundle["keyAlias"]):
        raise ValueError("Invalid alias")
    key_bytes = base64.b64decode(bundle["keystoreBase64"], validate=True)
    if not 100 <= len(key_bytes) <= 40000:
        raise ValueError("Invalid key size")
except (KeyError, ValueError, TypeError):
    sys.exit("NIA_SIGNING_BUNDLE is invalid. Upload the complete JSON created by scripts/setup_signing.py.")
for key in ["storePassword", "keyPassword", "keystoreBase64"]:
    print("::add-mask::" + bundle[key].replace("%", "%25"))
path = Path(os.environ["RUNNER_TEMP"]) / "nia-release.p12"
path.write_bytes(key_bytes)
path.chmod(0o600)
with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
    output.write("NIA_KEYSTORE_PATH=" + str(path) + "\n")
    output.write("NIA_STORE_PASSWORD=" + bundle["storePassword"] + "\n")
    output.write("NIA_KEY_ALIAS=" + bundle["keyAlias"] + "\n")
    output.write("NIA_KEY_PASSWORD=" + bundle["keyPassword"] + "\n")
print("Release signing configured.")
