#!/usr/bin/env python3
"""Bundle MapLibre style JSONs and sprite sheets as Android assets.

For each remote style, downloads the style JSON and the sprite sheets
(1x and 2x, JSON + PNG), rewrites the ``sprite`` field to an absolute
``asset://`` URL pointing at the bundled sprite directory, and writes
everything under ``app/src/main/assets/map-styles/<name>/``.

Tile sources, glyph URLs and any other remote references are left alone
so the basemap, labels and POI data continue to load over the network
when available.

Run:

    python3 app/bundle_map_styles.py [--force]

``--force`` re-downloads every file even if it already exists.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

STYLE_URLS: dict[str, str] = {
    "liberty":            "https://tiles.openfreemap.org/styles/liberty",
    "positron":           "https://tiles.openfreemap.org/styles/positron",
    "bright":             "https://tiles.openfreemap.org/styles/bright",
    "light":              "https://static.btcmap.org/map-styles/light.json",
    "dark":               "https://static.btcmap.org/map-styles/dark.json",
    "carto-dark-matter":  "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json",
}

SPRITE_URLS: dict[str, tuple[str, ...]] = {
    "ofm-sprites": (
        "https://tiles.openfreemap.org/sprites/ofm_f384/ofm.json",
        "https://tiles.openfreemap.org/sprites/ofm_f384/ofm.png",
        "https://tiles.openfreemap.org/sprites/ofm_f384/ofm@2x.json",
        "https://tiles.openfreemap.org/sprites/ofm_f384/ofm@2x.png",
    ),
    "carto-sprites": (
        "https://tiles.basemaps.cartocdn.com/gl/dark-matter-gl-style/sprite.json",
        "https://tiles.basemaps.cartocdn.com/gl/dark-matter-gl-style/sprite.png",
        "https://tiles.basemaps.cartocdn.com/gl/dark-matter-gl-style/sprite@2x.json",
        "https://tiles.basemaps.cartocdn.com/gl/dark-matter-gl-style/sprite@2x.png",
    ),
}

SPRITE_TARGET_NAMES: dict[str, str] = {
    "ofm.json":    "sprite.json",
    "ofm.png":     "sprite.png",
    "ofm@2x.json": "sprite@2x.json",
    "ofm@2x.png":  "sprite@2x.png",
}

STYLE_TO_SPRITE_BUNDLE: dict[str, str] = {
    "liberty":           "ofm-sprites",
    "positron":          "ofm-sprites",
    "bright":            "ofm-sprites",
    "light":             "ofm-sprites",
    "dark":              "ofm-sprites",
    "carto-dark-matter": "carto-sprites",
}

ASSETS_ROOT = Path(__file__).resolve().parent / "src" / "main" / "assets" / "map-styles"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "btcmap-android-bundler/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def write(path: Path, data: bytes, force: bool) -> bool:
    if path.exists() and not force:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return True


def bundle_style(name: str, url: str, force: bool) -> None:
    target_dir = ASSETS_ROOT / name
    target_json = target_dir / "style.json"
    print(f"[style] {name}: {url}")
    raw = fetch(url)
    style = json.loads(raw)

    sprite_bundle = STYLE_TO_SPRITE_BUNDLE[name]
    style["sprite"] = f"asset://map-styles/{sprite_bundle}/sprite"

    if "glyphs" not in style:
        print(f"  warning: style {name} has no glyphs field; labels will not render")

    style.pop("metadata", None)
    pretty = json.dumps(style, indent=2, ensure_ascii=False).encode("utf-8")
    if write(target_json, pretty, force):
        print(f"  wrote {target_json.relative_to(ASSETS_ROOT.parent.parent.parent)}")


def bundle_sprites(bundle: str, urls: tuple[str, ...], force: bool) -> None:
    target_dir = ASSETS_ROOT / bundle
    print(f"[sprite] {bundle}")
    for url in urls:
        original = Path(url).name
        target_name = SPRITE_TARGET_NAMES.get(original, original)
        target = target_dir / target_name
        if write(target, fetch(url), force):
            print(f"  wrote {target.relative_to(ASSETS_ROOT.parent.parent.parent)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="re-download existing files")
    args = parser.parse_args()

    ASSETS_ROOT.mkdir(parents=True, exist_ok=True)

    for bundle, urls in SPRITE_URLS.items():
        bundle_sprites(bundle, urls, args.force)

    for name, url in STYLE_URLS.items():
        bundle_style(name, url, args.force)

    print("Done.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, json.JSONDecodeError, OSError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
