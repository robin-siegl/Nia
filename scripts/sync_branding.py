#!/usr/bin/env python3
"""Export nia's approved vector masters; --check needs only Python's standard library."""

import argparse
import hashlib
import json
import re
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
BRAND = ROOT / "branding"
NS = "http://www.w3.org/2000/svg"
INK = "#171717"
PAPER = "#E6E6E6"
BLACK = "#000000"
ADAPTIVE_SCALE = 0.066


def paths(name):
    root = ET.parse(BRAND / "source" / f"nia-{name}.svg").getroot()
    found = root.findall(f"{{{NS}}}path")
    if len(found) != (2 if name == "mark" else 4):
        raise ValueError(f"Unexpected path count in the {name} master")
    return "\n".join("<path " + " ".join(
        f'{key}="{node.attrib[key]}"' for key in ("d", "fill-rule") if key in node.attrib
    ) + " />" for node in found)


def svg(viewbox, body, title):
    return (f'<svg xmlns="{NS}" viewBox="{viewbox}" role="img" aria-labelledby="title">\n'
            f'  <title id="title">{title}</title>\n{body}\n</svg>\n')


def mark(color):
    return f'<g fill="{color}">\n{paths("mark")}\n</g>'


def lockup(color):
    return (f'<g transform="scale(.6)">{mark(color)}</g>\n'
            f'<g transform="translate(600 100) scale(1.8)" fill="{color}">'
            f'{paths("wordmark")}</g>')


def app_art(foreground=PAPER, background=BLACK):
    # The 108dp layer's 52.8dp mark appears within Android's 72dp masked viewport.
    scale = 1000 * ADAPTIVE_SCALE / 72
    offset = 500 * (1 - scale)
    return (f'<rect width="1000" height="1000" fill="{background}" />\n'
            f'<g transform="translate({offset:.6f} {offset:.6f}) scale({scale:.9f})">'
            f'{mark(foreground)}</g>')


def android_path(data):
    """Keep native path data compact; the SVG master retains its full precision."""
    def number(match):
        value = match.group()
        negative = value.startswith("-")
        whole, _, fraction = value.lstrip("-").partition(".")
        tenths = int(whole) * 10 + int((fraction + "0")[0])
        if (fraction + "00")[1] >= "5":
            tenths += 1
        sign = "-" if negative and tenths else ""
        return sign + str(tenths // 10) + ("." + str(tenths % 10) if tenths % 10 else "")
    return re.sub(r"-?\d+(?:\.\d+)?", number, data)


def generated_text():
    output = {}
    for tone, color in (("dark", INK), ("light", PAPER)):
        output[f"branding/svg/nia-mark-{tone}.svg"] = svg("0 0 1000 1000", mark(color), "nia mark")
        output[f"branding/svg/nia-wordmark-{tone}.svg"] = svg("0 0 460 240",
            f'<g fill="{color}">{paths("wordmark")}</g>', "nia wordmark")
        output[f"branding/svg/nia-lockup-{tone}.svg"] = svg("0 0 1440 600", lockup(color), "nia")
    output["branding/svg/nia-app-icon.svg"] = svg("0 0 1000 1000", app_art(), "nia app icon")

    previews = []
    for index, (shape, foreground, background) in enumerate((
        ("circle", PAPER, BLACK), ("squircle", PAPER, BLACK), ("rounded", PAPER, BLACK),
        ("circle", "#351C55", "#ECDDFF"), ("squircle", "#ECDDFF", "#351C55"),
        ("rounded", INK, "#FFFFFF"),
    )):
        x = 24 + index * 196
        mask = ('<circle cx="500" cy="500" r="500" />' if shape == "circle" else
                '<rect width="1000" height="1000" rx="260" />' if shape == "rounded" else
                '<path d="M500 0 C920 0 1000 80 1000 500 C1000 920 920 1000 500 1000 '
                'C80 1000 0 920 0 500 C0 80 80 0 500 0 Z" />')
        previews.append(f'<g transform="translate({x} 32) scale(.16)">'
                        f'<clipPath id="mask-{index}">{mask}</clipPath>'
                        f'<g clip-path="url(#mask-{index})">{app_art(foreground, background)}</g></g>')
    output["branding/previews/icon-masks.svg"] = svg("0 0 1200 224", "\n".join(previews),
        "nia icon: circle, squircle, rounded square, and light and dark tint examples")

    android = "http://schemas.android.com/apk/res/android"
    entries = ET.parse(BRAND / "source/nia-mark.svg").getroot().findall(f"{{{NS}}}path")
    android_paths = "\n".join(f'    <path android:fillColor="{PAPER}" android:pathData="{android_path(p.attrib["d"])}" />'
                               for p in entries)
    output["app/src/main/res/drawable/ic_nia_mark.xml"] = (
        f'<vector xmlns:android="{android}" android:width="48dp" android:height="48dp" '
        'android:viewportWidth="1000" android:viewportHeight="1000">\n'
        f'{android_paths}\n</vector>\n')
    output["app/src/main/res/drawable/ic_nia_foreground.xml"] = (
        f'<vector xmlns:android="{android}" android:width="108dp" android:height="108dp" '
        'android:viewportWidth="108" android:viewportHeight="108">\n'
        f'  <group android:scaleX="{ADAPTIVE_SCALE}" android:scaleY="{ADAPTIVE_SCALE}" '
        f'android:translateX="{54 - 500 * ADAPTIVE_SCALE:g}" android:translateY="{54 - 500 * ADAPTIVE_SCALE:g}">\n'
        f'{android_paths}\n  </group>\n</vector>\n')
    # minSdk 29 supports adaptive icons; older parsers ignore the monochrome layer.
    output["app/src/main/res/mipmap-anydpi/ic_nia.xml"] = (
        f'<adaptive-icon xmlns:android="{android}">\n'
        '  <background android:drawable="@color/nia_icon_background" />\n'
        '  <foreground android:drawable="@drawable/ic_nia_foreground" />\n'
        '  <monochrome android:drawable="@drawable/ic_nia_foreground" />\n'
        '</adaptive-icon>\n')
    output["app/src/main/res/values/branding.xml"] = (
        '<resources>\n  <color name="nia_icon_background">#000000</color>\n</resources>\n')
    return output


def png_exports():
    exports = {}
    for tone in ("dark", "light"):
        exports[f"branding/png/nia-mark-{tone}-1024.png"] = (f"branding/svg/nia-mark-{tone}.svg", 1024)
        exports[f"branding/png/nia-wordmark-{tone}-1200.png"] = (f"branding/svg/nia-wordmark-{tone}.svg", 1200)
        exports[f"branding/png/nia-lockup-{tone}-1600.png"] = (f"branding/svg/nia-lockup-{tone}.svg", 1600)
    for size in (512, 1024):
        exports[f"branding/png/nia-app-icon-{size}.png"] = ("branding/svg/nia-app-icon.svg", size)
    exports["branding/previews/icon-masks.png"] = ("branding/previews/icon-masks.svg", 1200)
    return exports


def digest(path):
    return hashlib.sha256((ROOT / path).read_bytes()).hexdigest()


def manifest(text, png):
    return {"format": "nia.branding", "version": 1,
            "sources": {str(p.relative_to(ROOT)): digest(p.relative_to(ROOT))
                        for p in sorted((BRAND / "source").glob("*.svg"))},
            "files": {p: digest(p) for p in sorted(set(text) | set(png))}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify exports offline without rendering dependencies")
    args = parser.parse_args()
    text, png = generated_text(), png_exports()
    if args.check:
        stale = [p for p, contents in text.items() if not (ROOT / p).is_file()
                 or (ROOT / p).read_text() != contents]
        missing = [p for p in png if not (ROOT / p).is_file()]
        if stale or missing:
            raise ValueError("Brand exports need regeneration: " + ", ".join(stale + missing))
        expected = json.loads((BRAND / "manifest.json").read_text())
        if expected != manifest(text, png):
            raise ValueError("Brand export checksums differ; regenerate from the approved masters")
        print(f"Branding verified: 2 vector masters, {len(text)} text assets and {len(png)} PNG exports")
        return
    try:
        import cairosvg
    except ImportError:
        raise ValueError("Install scripts/branding-requirements.txt to regenerate PNG exports") from None
    for path, contents in text.items():
        target = ROOT / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(contents)
    for path, (source, width) in png.items():
        target = ROOT / path
        target.parent.mkdir(parents=True, exist_ok=True)
        cairosvg.svg2png(url=str(ROOT / source), write_to=str(target), output_width=width)
    (BRAND / "manifest.json").write_text(json.dumps(manifest(text, png), indent=2) + "\n")
    print("Updated brand exports and Android icon resources")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, ET.ParseError) as error:
        print(error, file=sys.stderr)
        sys.exit(1)
