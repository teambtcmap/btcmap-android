#!/usr/bin/env python3
"""Bundle MapLibre style JSONs, sprite sheets and font glyphs as Android assets.

For each remote style, downloads the style JSON and the sprite sheets it
declares (1x and 2x, JSON + PNG), rewrites the ``sprite`` field to an absolute
``asset://`` URL pointing at the bundled sprite directory, and writes
everything under ``app/src/main/assets/map-styles/<name>/``.

It also bundles the font glyphs the styles' labels need: every ``text-font`` in
every style is downloaded for each range in ``GLYPH_RANGES`` and the styles'
``glyphs`` field is rewritten to point at the bundled copies, so labels render
offline. See ``GLYPH_RANGES`` for the scripts that are covered.

The sprite source is taken from each style's own ``sprite`` field instead of a
hardcoded URL, so a sprite version bump upstream is picked up automatically.
Each sprite directory records the base it was downloaded from in
``source.json``; the sprites and glyphs are re-downloaded only when that base
changes or a file is missing, so a plain run does not re-fetch the large PNGs.

Tile sources and other remote references are left alone so the basemap and POI
data continue to load over the network when available, except for the styles in
``REBASED_ON_OPENFREEMAP`` (Carto Dark Matter), whose Carto source, sprite and
glyphs are swapped for OpenFreeMap's so they share the bundled basemap,
sprites and glyphs.

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
from urllib.parse import quote

STYLE_URLS: dict[str, str] = {
    "liberty":            "https://tiles.openfreemap.org/styles/liberty",
    "positron":           "https://tiles.openfreemap.org/styles/positron",
    "bright":             "https://tiles.openfreemap.org/styles/bright",
    "light":              "https://static.btcmap.org/map-styles/light.json",
    "dark":               "https://static.btcmap.org/map-styles/dark.json",
    "dark-matter":        "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json",
}

STYLE_TO_SPRITE_BUNDLE: dict[str, str] = {
    "liberty":           "ofm-sprites",
    "positron":          "ofm-sprites",
    "bright":            "ofm-sprites",
    "light":             "ofm-sprites",
    "dark":              "ofm-sprites",
    "dark-matter":       "ofm-sprites",
}

# Styles that are not served by OpenFreeMap but can be rebased onto its data
# (see `rebase_on_openfreemap`). Their upstream tile source, sprite and glyphs
# are replaced, so the values below are not read from the fetched style.
REBASED_ON_OPENFREEMAP = {"dark-matter"}

OPENMAPTILES_SOURCE = "openmaptiles"
CARTO_SOURCE = "carto"

# The OpenFreeMap endpoints the rebased styles are pointed at. They are the
# same ones the OpenFreeMap-hosted styles declare, and the checks below fail
# loudly if upstream moves them.
OPENFREEMAP_PLANET_URL = "https://tiles.openfreemap.org/planet"
OPENFREEMAP_SPRITE_URL = "https://tiles.openfreemap.org/sprites/ofm_f384/ofm"
OPENFREEMAP_GLYPHS_URL = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

# MapLibre resolves a sprite base URL to three files; we store them under a
# stable name and rewrite every style to point at that name.
SPRITE_TARGET_NAME = "sprite"
SPRITE_SUFFIXES = (".json", ".png", "@2x.json", "@2x.png")
SPRITE_SOURCE_FILE = "source.json"

# The glyph ranges bundled for the styles' labels. They cover every script the
# styles' labels use except the CJK ideograph and Hangul syllable blocks, which
# alone are around 90 MB; labels that fall back to those stay online-only. Each
# range is a block of 256 code points. Surrogate and private-use blocks are
# skipped because they carry no glyphs.
GLYPH_RANGES = (
    "0-255",
    "256-511",
    "512-767",
    "768-1023",
    "1024-1279",
    "1280-1535",
    "1536-1791",
    "1792-2047",
    "2048-2303",
    "2304-2559",
    "2560-2815",
    "2816-3071",
    "3072-3327",
    "3328-3583",
    "3584-3839",
    "3840-4095",
    "4096-4351",
    "4352-4607",
    "4608-4863",
    "4864-5119",
    "5120-5375",
    "5376-5631",
    "5632-5887",
    "5888-6143",
    "6144-6399",
    "6400-6655",
    "6656-6911",
    "6912-7167",
    "7168-7423",
    "7424-7679",
    "7680-7935",
    "7936-8191",
    "8192-8447",
    "8448-8703",
    "8704-8959",
    "8960-9215",
    "9216-9471",
    "9472-9727",
    "9728-9983",
    "9984-10239",
    "10240-10495",
    "10496-10751",
    "10752-11007",
    "11008-11263",
    "11264-11519",
    "11520-11775",
    "11776-12031",
    "12032-12287",
    "12288-12543",
    "12544-12799",
    "12800-13055",
    "13056-13311",
    "40960-41215",
    "41216-41471",
    "41472-41727",
    "41728-41983",
    "41984-42239",
    "42240-42495",
    "42496-42751",
    "42752-43007",
    "43008-43263",
    "43264-43519",
    "43520-43775",
    "43776-44031",
    "64256-64511",
    "64512-64767",
    "64768-65023",
    "65024-65279",
    "65280-65535",
)
GLYPH_DIR = "glyphs"
GLYPH_TARGET_URL = f"asset://map-styles/{GLYPH_DIR}/{{fontstack}}/{{range}}.pbf"
GLYPH_SOURCE_FILE = "source.json"

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


def noto_sans_variant(fonts: list[str]) -> str:
    """The bundled Noto Sans face that best stands in for a style's font stack."""
    joined = " ".join(fonts)
    if "Italic" in joined:
        return "Noto Sans Italic"
    if "Bold" in joined or "Medium" in joined:
        return "Noto Sans Bold"
    return "Noto Sans Regular"


def rebase_on_openfreemap(style: dict) -> None:
    """Rewrites a Carto style so it draws OpenFreeMap's tiles and assets.

    Carto's vector tiles use the same layer names and fields as OpenMapTiles,
    which OpenFreeMap serves, so only the source is swapped. Carto's label
    fonts are not hosted by OpenFreeMap, so every text-font stack is collapsed
    to the Noto Sans variant that is bundled, and the one sprite icon the style
    names is pointed at the bundled equivalent.
    """
    style["sources"] = {
        OPENMAPTILES_SOURCE: {"type": "vector", "url": OPENFREEMAP_PLANET_URL},
    }
    style["sprite"] = OPENFREEMAP_SPRITE_URL
    style["glyphs"] = OPENFREEMAP_GLYPHS_URL

    for layer in style.get("layers", []):
        if layer.get("source") == CARTO_SOURCE:
            layer["source"] = OPENMAPTILES_SOURCE

        layout = layer.get("layout")
        if not isinstance(layout, dict):
            continue

        fonts = layout.get("text-font")
        if isinstance(fonts, list) and fonts:
            layout["text-font"] = [noto_sans_variant(fonts)]

        # Carto spells it with a hyphen; the bundled sprite uses an underscore.
        if layout.get("icon-image") == "circle-11":
            layout["icon-image"] = "circle_11"

    # Every layer must now draw from the OpenFreeMap source; a layer left on
    # another one would silently render nothing, so fail the bundle instead.
    dangling = sorted({
        layer["source"]
        for layer in style.get("layers", [])
        if isinstance(layer.get("source"), str) and layer["source"] != OPENMAPTILES_SOURCE
    })
    if dangling:
        raise RuntimeError(f"rebase left layers on unknown sources: {dangling}")


def bundle_style(name: str, style: dict) -> None:
    target = ASSETS_ROOT / name / "style.json"
    sprite_bundle = STYLE_TO_SPRITE_BUNDLE[name]
    style["sprite"] = f"asset://map-styles/{sprite_bundle}/{SPRITE_TARGET_NAME}"

    if "glyphs" in style:
        style["glyphs"] = GLYPH_TARGET_URL
    else:
        print(f"  warning: style {name} has no glyphs field; labels will not render")

    style.pop("metadata", None)
    pretty = json.dumps(style, indent=2, ensure_ascii=False).encode("utf-8")
    write_atomic(target, pretty)
    print(f"  wrote {relative(target)}")


def style_fonts(style: dict) -> set[str]:
    """The font names the style's label layers ask for."""
    fonts: set[str] = set()
    for layer in style.get("layers", []):
        for font in layer.get("layout", {}).get("text-font", []) or []:
            fonts.add(font)
    return fonts


def glyph_base(glyphs_url: str) -> str:
    """The part of a style's glyphs URL before ``{fontstack}``."""
    marker = "{fontstack}"
    if marker not in glyphs_url:
        raise RuntimeError(f"glyphs URL has no {{fontstack}} token: {glyphs_url}")
    return glyphs_url.split(marker, 1)[0]


def read_glyph_source() -> dict | None:
    manifest = ASSETS_ROOT / GLYPH_DIR / GLYPH_SOURCE_FILE
    if not manifest.exists():
        return None
    try:
        return json.loads(manifest.read_text())
    except (OSError, json.JSONDecodeError):
        return None


def bundle_glyphs(base: str, fonts: set[str], force: bool) -> None:
    manifest = {"base": base, "ranges": list(GLYPH_RANGES)}
    source_changed = read_glyph_source() != manifest
    print(f"[glyphs] {base} ({len(fonts)} fonts x {len(GLYPH_RANGES)} ranges)")

    for font in sorted(fonts):
        for glyph_range in GLYPH_RANGES:
            target = ASSETS_ROOT / GLYPH_DIR / font / f"{glyph_range}.pbf"
            if target.exists() and not force and not source_changed:
                continue
            write_atomic(target, fetch(f"{base}{quote(font)}/{glyph_range}.pbf"))

    write_atomic(
        ASSETS_ROOT / GLYPH_DIR / GLYPH_SOURCE_FILE,
        json.dumps(manifest, indent=2).encode("utf-8") + b"\n",
    )


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
    glyph_bases: set[str] = set()
    fonts: set[str] = set()

    # Styles are small, so they are always fetched: that is how a changed
    # upstream sprite or glyph base is noticed. The large sprite sheets and
    # glyph files are only downloaded when their recorded source is stale or a
    # file is missing.
    for name, url in STYLE_URLS.items():
        print(f"[style] {name}: {url}")
        style = json.loads(fetch(url))

        if name in REBASED_ON_OPENFREEMAP:
            rebase_on_openfreemap(style)

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

        glyphs_url = style.get("glyphs")
        if isinstance(glyphs_url, str) and glyphs_url:
            glyph_bases.add(glyph_base(glyphs_url))
        fonts |= style_fonts(style)

        bundle_style(name, style)

    if len(glyph_bases) != 1:
        raise RuntimeError(f"styles disagree on the glyph base URL: {sorted(glyph_bases)}")

    for bundle, base in sprite_sources.items():
        source_changed = read_sprite_source(bundle) != base
        if not args.force and not source_changed and sprite_bundle_is_complete(bundle):
            print(f"[sprite] {bundle}: up to date")
            continue
        bundle_sprites(bundle, base, force=args.force or source_changed)

    bundle_glyphs(next(iter(glyph_bases)), fonts, force=args.force)

    print("Done.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, json.JSONDecodeError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
