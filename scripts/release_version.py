#!/usr/bin/env python3
"""Resolve a new stable release from Git tags and the version in gradle.properties."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

VERSION = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")


def parse_version(text):
    match = VERSION.fullmatch(text)
    if not match:
        raise ValueError("Use a stable version such as 1.0.0, without a v prefix or prerelease suffix.")
    value = tuple(map(int, match.groups()))
    if value[0] > 1999 or value[1] > 999 or value[2] > 999:
        raise ValueError("Version limits: major <= 1999, minor and patch <= 999. Choose a larger bump if needed.")
    return value


def resolve_version(minimum, tags, bump="patch", requested=""):
    if bump not in ("patch", "minor", "major"):
        raise ValueError("Choose patch, minor or major.")
    floor = parse_version(minimum)
    stable = []
    for tag in tags:
        if not tag.startswith("v"):
            continue
        try:
            stable.append(parse_version(tag[1:]))
        except ValueError:
            continue
    latest = max(stable, default=None)
    if requested.strip():
        chosen = parse_version(requested.strip())
        if chosen < floor:
            raise ValueError("The requested version is below the source version " + minimum + ".")
    elif latest is None:
        chosen = floor
    else:
        major, minor, patch = latest
        next_version = {"patch": (major, minor, patch + 1), "minor": (major, minor + 1, 0),
                        "major": (major + 1, 0, 0)}[bump]
        chosen = max(floor, next_version)
    text = ".".join(map(str, chosen))
    parse_version(text)
    if latest is not None and chosen <= latest:
        raise ValueError("Choose a version newer than v" + ".".join(map(str, latest)) + ". Existing tags are never replaced.")
    code = chosen[0] * 1_000_000 + chosen[1] * 1_000 + chosen[2]
    if code < 1:
        raise ValueError("The release version must produce a positive Android versionCode.")
    return {"version": text, "tag": "v" + text, "version_code": code}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default=os.environ.get("RELEASE_VERSION", ""))
    parser.add_argument("--bump", default=os.environ.get("RELEASE_BUMP", "") or "patch")
    parser.add_argument("--properties", default="gradle.properties")
    args = parser.parse_args()
    if os.environ.get("GITHUB_ACTIONS") == "true" and os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise ValueError("Production builds must run from the main branch.")
    properties = dict(line.split("=", 1) for line in Path(args.properties).read_text().splitlines()
                      if "=" in line and not line.lstrip().startswith("#"))
    minimum = properties["nia.versionName"].strip()
    tags = subprocess.check_output(["git", "tag", "--list"], text=True).splitlines()
    result = resolve_version(minimum, tags, args.bump, args.version)
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for name, value in result.items():
                output.write(f"{name}={value}\n")
    print(json.dumps(result))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        sys.exit("Release version could not be resolved: " + str(error))
