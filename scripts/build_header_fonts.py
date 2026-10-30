#!/usr/bin/env python3
"""Build the bundled clock fonts from the verified Google Fonts Google Sans source.

Development helper only; APK builds use the committed fonts and need no FontTools.
Requires FontTools. Pass --source to the upstream TTF, with OFL.txt and TRADEMARKS.md beside it.
"""
import argparse
import hashlib
from pathlib import Path
import shutil

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

SOURCE_BLOB = "a5e76cc713206f1cfe9a7fc31cc1b6f035034d81"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    args = parser.parse_args()
    source = args.source.read_bytes()
    digest = hashlib.sha1(b"blob " + str(len(source)).encode() + b"\0" + source).hexdigest()
    if digest != SOURCE_BLOB:
        raise SystemExit("The source does not match the pinned Google Fonts Git blob.")
    root = Path(__file__).resolve().parents[1]
    destination = root / "app/src/main/res/font"
    destination.mkdir(parents=True, exist_ok=True)
    characters = set(range(0x20, 0x250)) | set(range(0x2000, 0x2070)) | set(range(0x660, 0x66A)) | set(range(0x6F0, 0x6FA))
    for filename, style, weight in [("nia_header_regular.ttf", "Regular", 400), ("nia_header_semibold.ttf", "SemiBold", 650)]:
        font = TTFont(args.source, recalcTimestamp=False)
        font = instantiateVariableFont(font, {"opsz": 18, "wght": weight, "GRAD": 0}, inplace=True)
        names = {1: "Nia Header", 2: style, 3: "NiaHeader-" + style, 4: "Nia Header " + style,
                 6: "NiaHeader-" + style, 16: "Nia Header", 17: style}
        for record in font["name"].names:
            if record.nameID in names:
                record.string = names[record.nameID].encode(record.getEncoding(), errors="replace")
        options = subset.Options()
        options.name_IDs = ["*"]
        options.name_legacy = True
        options.name_languages = ["*"]
        subsetter = subset.Subsetter(options=options)
        subsetter.populate(unicodes=characters)
        subsetter.subset(font)
        target = destination / filename
        font.save(target)
        print(filename, target.stat().st_size, "bytes", hashlib.sha256(target.read_bytes()).hexdigest())
    licenses = root / "app/src/main/assets/licenses"
    licenses.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(args.source.parent / "OFL.txt", licenses / "google-sans-OFL.txt")
    shutil.copyfile(args.source.parent / "TRADEMARKS.md", licenses / "google-sans-TRADEMARKS.md")


if __name__ == "__main__":
    main()
