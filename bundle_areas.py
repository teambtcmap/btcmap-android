#!/usr/bin/env python3
"""Download the latest areas snapshot as a bundled Android asset.

Fetches every area from the BTC Map API and writes it to
``app/src/main/assets/bundled-areas.json``. Like the places snapshot, it carries
the full field set the app syncs, including the full ``geo_json`` polygon and
each area's real ``updated_at``, so a seeded row is a complete record. The first
sync therefore only has to fetch the delta since the snapshot was generated, and
community and country chips work offline (and while the server is unreachable)
without downloading megabytes of polygons first.

``deleted_at`` is deliberately not requested: requesting it also makes the API
return soft-deleted tombstones, which the bundle does not need because an area
that was deleted before the snapshot was built is simply absent from it.

The output is pretty-printed and sorted by id. This keeps the diff of a
refresh limited to the areas that actually changed instead of rewriting the
whole file.

Run:

    python3 bundle_areas.py

The latest snapshot is always fetched, replacing any existing asset.
"""

import datetime
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

# The full field set the app syncs, minus ``deleted_at`` (see the module
# docstring). Keep this in sync with ``AREA_DELTA_FIELDS`` in AreaApi.kt and
# with ``readBundledArea`` in BundledAreas.kt.
FIELDS = (
    "name",
    "type",
    "url_alias",
    "icon",
    "icon_wide",
    "website_url",
    "description",
    "bbox",
    "geo_json",
    "updated_at",
)

API_URL = "https://api.btcmap.org/v4/areas?fields=" + ",".join(FIELDS)
PROJECT_ROOT = Path(__file__).resolve().parent
APP_DIR = PROJECT_ROOT / "app"
OUTPUT_FILE = APP_DIR / "src" / "main" / "assets" / "bundled-areas.json"

REQUIRED_FIELDS = ("id", "name", "type", "url_alias", "website_url", "updated_at")

# Optional free-text fields.
OPTIONAL_STRING_FIELDS = ("icon", "icon_wide", "description")


def user_agent() -> str:
    try:
        build_script = (APP_DIR / "build.gradle.kts").read_text()
        match = re.search(r"versionCode\s*=\s*(\d+)", build_script)
        if match:
            return f"BTC Map Android {match.group(1)}"
    except OSError:
        pass
    return "BTC Map Android"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": user_agent(), "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def _is_number(value: object) -> bool:
    # bool is a subclass of int, but a boolean number is a bug.
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _is_timestamp(value: object) -> bool:
    if not isinstance(value, str) or not value:
        return False
    try:
        datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return False
    return True


def validate(areas: list) -> None:
    """Reject an unexpected shape with a clear message instead of a traceback.

    Field presence, types and geometry shape are checked here so a bad download
    fails at bundling time, rather than on-device where a malformed snapshot
    forces the importer to roll back the whole seed.
    """
    if not isinstance(areas, list):
        raise RuntimeError("downloaded areas are not a JSON array")
    if not areas:
        raise RuntimeError("downloaded areas are empty")
    for index, area in enumerate(areas):
        if not isinstance(area, dict):
            raise RuntimeError(f"area at index {index} is not a JSON object")
        for field in REQUIRED_FIELDS:
            if field not in area:
                raise RuntimeError(f"area at index {index} is missing '{field}'")

        area_id = area["id"]
        if not isinstance(area_id, int) or isinstance(area_id, bool):
            raise RuntimeError(f"area at index {index} has a non-integer 'id'")

        # The planet area (id 662) legitimately has an empty name, so only the
        # type is required to be non-empty alongside the identifiers below.
        name = area["name"]
        if not isinstance(name, str):
            raise RuntimeError(f"area {area_id} has a non-string 'name'")

        for field in ("type", "url_alias", "website_url"):
            value = area[field]
            if not isinstance(value, str) or not value:
                raise RuntimeError(f"area {area_id} has a non-string or empty '{field}'")

        # The seed's whole point is the delta sync, which pages from the newest
        # stored ``updated_at``: an unparseable one would corrupt that cursor.
        if not _is_timestamp(area["updated_at"]):
            raise RuntimeError(f"area {area_id} has an invalid 'updated_at'")

        for field in OPTIONAL_STRING_FIELDS:
            value = area.get(field)
            if value is not None and not isinstance(value, str):
                raise RuntimeError(f"area {area_id} has a non-string '{field}'")

        # The app stores bbox as four separate columns, so the snapshot must
        # carry exactly west, south, east, north or nothing at all.
        bbox = area.get("bbox")
        if bbox is not None:
            if not isinstance(bbox, list) or len(bbox) != 4 or not all(_is_number(v) for v in bbox):
                raise RuntimeError(f"area {area_id} has a 'bbox' that is not four numbers")

        # geo_json is stored verbatim as the serialized polygon.
        geo_json = area.get("geo_json")
        if geo_json is not None and not isinstance(geo_json, dict):
            raise RuntimeError(f"area {area_id} has a non-object 'geo_json'")


def main() -> int:
    raw = fetch(API_URL)
    try:
        areas = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("downloaded areas are not valid JSON") from exc

    validate(areas)
    areas.sort(key=lambda area: area["id"])
    pretty = json.dumps(areas, indent=2, ensure_ascii=False).encode("utf-8") + b"\n"

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    # Write atomically so a failed download never leaves a truncated asset, and
    # remove the temporary file if the write or rename fails.
    tmp_file = OUTPUT_FILE.with_name(OUTPUT_FILE.name + ".tmp")
    try:
        tmp_file.write_bytes(pretty)
        tmp_file.replace(OUTPUT_FILE)
    except BaseException:
        tmp_file.unlink(missing_ok=True)
        raise

    print(f"Bundled {len(areas)} areas into {OUTPUT_FILE.relative_to(APP_DIR.parent)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
