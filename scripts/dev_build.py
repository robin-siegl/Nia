#!/usr/bin/env python3
"""Identify and package a manually requested nia Dev APK without publishing a release."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

from release_version import read_project_version, validate_project_version

REPOSITORY = "robin-siegl/Nia"
APPLICATION_ID = "dev.robinsiegl.nia.dev"


def build_metadata(version, code, env):
    code = validate_project_version(version, code)
    if env.get("GITHUB_REPOSITORY") != REPOSITORY or env.get("GITHUB_EVENT_NAME") != "workflow_dispatch":
        raise ValueError(f"Dev APK builds must be started manually in {REPOSITORY}.")
    ref = env.get("GITHUB_REF", "")
    if not re.fullmatch(r"refs/heads/[^\s\x00-\x1f\x7f]+", ref):
        raise ValueError("Select a branch when running Dev APK.")
    commit = env.get("GITHUB_SHA", "")
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("The source commit must be a full GitHub commit SHA.")
    numbers = {}
    for key in ("GITHUB_RUN_NUMBER", "GITHUB_RUN_ATTEMPT", "GITHUB_RUN_ID"):
        value = env.get(key, "")
        if not re.fullmatch(r"[1-9][0-9]*", value):
            raise ValueError(f"{key} must be a positive whole number.")
        numbers[key] = value
    build_id = numbers["GITHUB_RUN_NUMBER"] + "." + numbers["GITHUB_RUN_ATTEMPT"]
    name = f"nia-{version}-dev-{build_id}-{commit[:7]}"
    return {
        "version": version, "version_code": code,
        "version_name": f"{version}-dev.{build_id}+{commit[:7]}",
        "build_id": build_id, "commit_short": commit[:7], "commit": commit,
        "source_ref": ref, "application_id": APPLICATION_ID,
        "run_url": f"https://github.com/{REPOSITORY}/actions/runs/{numbers['GITHUB_RUN_ID']}",
        "artifact_name": name, "apk_name": name + ".apk",
    }


def read_metadata(properties):
    metadata = build_metadata(*read_project_version(properties), os.environ)
    checked_out = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    if checked_out != metadata["commit"]:
        raise ValueError("The checked-out commit does not match the requested build.")
    return metadata


def validate_apk_identity(badging, metadata):
    package = next((line for line in badging.splitlines() if line.startswith("package:")), "")
    values = dict(re.findall(r"([A-Za-z]+)='([^']*)'", package))
    if (values.get("name") != APPLICATION_ID or values.get("versionCode") != str(metadata["version_code"]) or
            values.get("versionName") != metadata["version_name"]):
        raise ValueError("The APK must be nia Dev with the committed version and requested build identity.")
    if "application-label:'nia Dev'" not in badging.splitlines():
        raise ValueError("The APK must be labeled nia Dev.")
    if re.search(r"^application-debuggable\b", badging, re.MULTILINE):
        raise ValueError("nia Dev must use the optimized, non-debuggable build type.")


def verify_apk(apk, metadata):
    sdk = os.environ.get("ANDROID_HOME")
    if not sdk:
        raise ValueError("ANDROID_HOME must point to Android SDK Build Tools 36.0.0.")
    tools = Path(sdk) / "build-tools/36.0.0"
    badging = subprocess.check_output([str(tools / "aapt2"), "dump", "badging", str(apk)], text=True)
    validate_apk_identity(badging, metadata)
    result = subprocess.run([str(tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)],
                            check=True, capture_output=True, text=True)
    fingerprints = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})$",
                              result.stdout, re.MULTILINE | re.IGNORECASE)
    if len(fingerprints) != 1:
        raise ValueError("The APK must have one verified signing certificate.")
    return fingerprints[0].lower()


def prepare_download(apk, output, metadata, signer):
    # A fresh directory prevents an old or unrelated APK from entering the artifact.
    output.mkdir(parents=True, exist_ok=False)
    target = output / metadata["apk_name"]
    shutil.copyfile(apk, target)
    with target.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    info = dict(metadata, apk_sha256=digest, signer_sha256=signer, variant="dev")
    (output / "BUILD_INFO.json").write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
    (output / "SHA256SUMS.txt").write_text(f"{digest}  {target.name}\n", encoding="utf-8")
    return target


def write_summary(metadata, artifact_url):
    expected = re.escape(metadata["run_url"]) + r"/artifacts/[1-9][0-9]*"
    if not re.fullmatch(expected, artifact_url):
        raise ValueError("The download must be an artifact from this workflow run.")
    text = (f"### nia Dev {metadata['version_name']}\n\n"
            f"[Download the APK bundle]({artifact_url})\n\n"
            f"Extract the ZIP and install `{metadata['apk_name']}`. Downloads are kept for 30 days and require GitHub sign-in.\n\n"
            "nia Dev installs alongside nia with separate settings. Select it as your Home app while testing; "
            "import a settings backup to copy your setup. Later dev builds update this installation.\n\n"
            f"Source: [`{metadata['commit_short']}`](https://github.com/{REPOSITORY}/commit/{metadata['commit']}). "
            "The bundle includes `SHA256SUMS.txt` and `BUILD_INFO.json`. No tag or release was created.\n")
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as stream:
        stream.write(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("metadata", "package", "summary"))
    parser.add_argument("--properties", default="gradle.properties")
    parser.add_argument("--apk", type=Path, default=Path("app/build/outputs/apk/dev/app-dev.apk"))
    parser.add_argument("--output", type=Path, default=Path("dist/dev"))
    args = parser.parse_args()
    metadata = read_metadata(args.properties)
    if args.command == "metadata":
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
            for key in ("version", "version_code", "build_id", "commit_short", "artifact_name"):
                stream.write(f"{key}={metadata[key]}\n")
        print(json.dumps(metadata))
    elif args.command == "package":
        signer = verify_apk(args.apk, metadata)
        target = prepare_download(args.apk, args.output, metadata, signer)
        print(f"Verified signed nia Dev APK: {target.name}")
    else:
        write_summary(metadata, os.environ.get("NIA_DEV_ARTIFACT_URL", ""))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        sys.exit("Dev APK preparation failed: " + str(error))
