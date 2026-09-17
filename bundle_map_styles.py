#!/usr/bin/env python3
"""Bundle MapLibre style JSONs and sprite sheets as Android assets.

For each remote style, downloads the style JSON and the sprite sheets it
declares (1x and 2x, JSON + PNG), rewrites the ``sprite`` field to an absolute
``asset://`` URL pointing at the bundled sprite directory, and writes
everything under ``app/src/main/assets/map-styles/<name>/``.

The sprite source is taken from each style's own ``sprite`` field instead of a
hardcoded URL, so a sprite version bump upstream is picked up automatically.
Each sprite directory records the base it was downloaded from in
``source.json``; the sprites are re-downloaded only when that base changes or a
file is missing, so a plain run does not re-fetch the large PNGs.

Tile sources, glyph URLs and any other remote references are left alone
so the basemap, labels and POI data continue to load over the network
when available.

Run:

    python3 bundle_map_styles.py [--force]

``--force`` re-downloads every file even if it already exists.
"""

import argparse
import json
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

STYLE_TO_SPRITE_BUNDLE: dict[str, str] = {
    "liberty":           "ofm-sprites",
    "positron":          "ofm-sprites",
    "bright":            "ofm-sprites",
    "light":             "ofm-sprites",
    "dark":              "ofm-sprites",
    "carto-dark-matter": "carto-sprites",
}

# MapLibre resolves a sprite base URL to three files; we store them under a
# stable name and rewrite every style to point at that name.
SPRITE_TARGET_NAME = "sprite"
SPRITE_SUFFIXES = (".json", ".png", "@2x.json", "@2x.png")
SPRITE_SOURCE_FILE = "source.json"

PROJECT_ROOT = Path(__file__).resolve().parent
ASSETS_ROOT = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "map-styles"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "btcmap-android-bundler/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def write_atomic(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp")
    try:
        tmp.write_bytes(data)
        tmp.replace(path)
    except BaseException:
        tmp.unlink(missing_ok=True)
        raise


def relative(path: Path) -> Path:
    return path.relative_to(PROJECT_ROOT)


def sprite_path(bundle: str, suffix: str) -> Path:
    return ASSETS_ROOT / bundle / f"{SPRITE_TARGET_NAME}{suffix}"


def read_sprite_source(bundle: str) -> str | None:
    manifest = ASSETS_ROOT / bundle / SPRITE_SOURCE_FILE
    if not manifest.exists():
        return None
    try:
        return json.loads(manifest.read_text()).get("sprite")
    except (OSError, json.JSONDecodeError):
        return None


def sprite_bundle_is_complete(bundle: str) -> bool:
    return all(sprite_path(bundle, suffix).exists() for suffix in SPRITE_SUFFIXES)


def bundle_style(name: str, style: dict) -> None:
    target = ASSETS_ROOT / name / "style.json"
    sprite_bundle = STYLE_TO_SPRITE_BUNDLE[name]
    style["sprite"] = f"asset://map-styles/{sprite_bundle}/{SPRITE_TARGET_NAME}"

    if "glyphs" not in style:
        print(f"  warning: style {name} has no glyphs field; labels will not render")

    style.pop("metadata", None)
    pretty = json.dumps(style, indent=2, ensure_ascii=False).encode("utf-8")
    write_atomic(target, pretty)
    print(f"  wrote {relative(target)}")


def bundle_sprites(bundle: str, base: str, force: bool) -> None:
    print(f"[sprite] {bundle}: {base}")
    for suffix in SPRITE_SUFFIXES:
        target = sprite_path(bundle, suffix)
        if target.exists() and not force:
            continue
        write_atomic(target, fetch(f"{base}{suffix}"))
        print(f"  wrote {relative(target)}")

    manifest = json.dumps({"sprite": base}, indent=2).encode("utf-8") + b"\n"
    write_atomic(ASSETS_ROOT / bundle / SPRITE_SOURCE_FILE, manifest)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="re-download existing files")
    args = parser.parse_args()

    ASSETS_ROOT.mkdir(parents=True, exist_ok=True)

    sprite_sources: dict[str, str] = {}

    # Styles are small, so they are always fetched: that is how a changed
    # upstream sprite base is noticed. The large sprite sheets are only
    # downloaded when their recorded source is stale or a file is missing.
    for name, url in STYLE_URLS.items():
        print(f"[style] {name}: {url}")
        style = json.loads(fetch(url))

        base = style.get("sprite")
        if not isinstance(base, str) or not base:
            raise RuntimeError(f"style {name} does not declare a sprite base URL")

        bundle = STYLE_TO_SPRITE_BUNDLE[name]
        previous = sprite_sources.setdefault(bundle, base)
        if previous != base:
            raise RuntimeError(
                f"styles sharing sprite bundle '{bundle}' disagree on base URL: "
                f"{previous} vs {base}"
            )

        bundle_style(name, style)

    for bundle, base in sprite_sources.items():
        source_changed = read_sprite_source(bundle) != base
        if not args.force and not source_changed and sprite_bundle_is_complete(bundle):
            print(f"[sprite] {bundle}: up to date")
            continue
        bundle_sprites(bundle, base, force=args.force or source_changed)

    print("Done.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, json.JSONDecodeError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
