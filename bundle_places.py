#!/usr/bin/env python3
"""Download the latest places snapshot as a bundled Android asset.

Fetches every place from the BTC Map API and writes it to
``app/src/main/assets/bundled-places.json``. The snapshot carries the full field
set the app syncs, including each place's real ``updated_at``, so a seeded row
is a complete record rather than a placeholder. The first sync therefore only
has to fetch the delta since the snapshot was generated, and the map stays fully
usable offline or while the server is unreachable.

``deleted_at`` is deliberately not requested: asking for it also makes the API
return soft-deleted tombstones, which the bundle does not need because a place
that was deleted before the snapshot was built is simply absent from it.

The output is pretty-printed and sorted by id. This keeps the diff of a
refresh limited to the places that actually changed instead of rewriting the
whole file.

Run:

    python3 bundle_places.py

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
# docstring). Keep this in sync with ``placeFields`` in PlaceApi.kt and with
# ``readBundledPlace`` in BundledPlaces.kt.
FIELDS = (
    "lat",
    "lon",
    "icon",
    "name",
    "localized_name",
    "updated_at",
    "required_app_url",
    "boosted_until",
    "verified_at",
    "address",
    "opening_hours",
    "localized_opening_hours",
    "website",
    "phone",
    "email",
    "twitter",
    "facebook",
    "instagram",
    "line",
    "comments",
    "telegram",
    "osm_id",
)

API_URL = "https://api.btcmap.org/v4/places?fields=" + ",".join(FIELDS)
PROJECT_ROOT = Path(__file__).resolve().parent
APP_DIR = PROJECT_ROOT / "app"
OUTPUT_FILE = APP_DIR / "src" / "main" / "assets" / "bundled-places.json"

REQUIRED_FIELDS = ("id", "lat", "lon", "icon", "updated_at")

# Fields stored as free text or URLs by the app, all optional.
OPTIONAL_STRING_FIELDS = (
    "name",
    "required_app_url",
    "boosted_until",
    "verified_at",
    "address",
    "opening_hours",
    "website",
    "phone",
    "email",
    "twitter",
    "facebook",
    "instagram",
    "line",
    "telegram",
    "osm_id",
)

OPTIONAL_OBJECT_FIELDS = ("localized_name", "localized_opening_hours")


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
    # bool is a subclass of int, but a boolean coordinate or id is a bug.
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _is_timestamp(value: object) -> bool:
    if not isinstance(value, str) or not value:
        return False
    try:
        datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return False
    return True


def validate(places: list) -> None:
    """Reject an unexpected shape with a clear message instead of a traceback.

    Field presence, types and coordinate ranges are checked here so a bad
    download fails at bundling time, rather than on-device where a malformed
    snapshot forces the importer to roll back the whole seed.
    """
    if not isinstance(places, list):
        raise RuntimeError("downloaded places are not a JSON array")
    if not places:
        raise RuntimeError("downloaded places are empty")
    for index, place in enumerate(places):
        if not isinstance(place, dict):
            raise RuntimeError(f"place at index {index} is not a JSON object")
        for field in REQUIRED_FIELDS:
            if field not in place:
                raise RuntimeError(f"place at index {index} is missing '{field}'")

        place_id = place["id"]
        if not isinstance(place_id, int) or isinstance(place_id, bool):
            raise RuntimeError(f"place at index {index} has a non-integer 'id'")

        lat = place["lat"]
        lon = place["lon"]
        if not _is_number(lat):
            raise RuntimeError(f"place {place_id} has a non-numeric 'lat'")
        if not _is_number(lon):
            raise RuntimeError(f"place {place_id} has a non-numeric 'lon'")
        if not -90 <= lat <= 90:
            raise RuntimeError(f"place {place_id} has a 'lat' outside [-90, 90]")
        if not -180 <= lon <= 180:
            raise RuntimeError(f"place {place_id} has a 'lon' outside [-180, 180]")

        icon = place["icon"]
        if not isinstance(icon, str) or not icon:
            raise RuntimeError(f"place {place_id} has a non-string or empty 'icon'")

        # The seed's whole point is the delta sync, which pages from the newest
        # stored ``updated_at``: an unparseable one would corrupt that cursor.
        if not _is_timestamp(place["updated_at"]):
            raise RuntimeError(f"place {place_id} has an invalid 'updated_at'")

        for field in OPTIONAL_STRING_FIELDS:
            value = place.get(field)
            if value is not None and not isinstance(value, str):
                raise RuntimeError(f"place {place_id} has a non-string '{field}'")

        for field in OPTIONAL_OBJECT_FIELDS:
            value = place.get(field)
            if value is not None and not isinstance(value, dict):
                raise RuntimeError(f"place {place_id} has a non-object '{field}'")

        comments = place.get("comments")
        if comments is not None and (not isinstance(comments, int) or isinstance(comments, bool)):
            raise RuntimeError(f"place {place_id} has a non-integer 'comments'")

        # URL fields are only type-checked, not required to be absolute: the
        # API does return values like "www.example.com" or even "yes", and the
        # app already maps those to null with HttpUrl.toHttpUrlOrNull(). The
        # importer has to tolerate them, so the bundler must not reject the
        # whole snapshot over data the app would silently drop anyway.


def main() -> int:
    raw = fetch(API_URL)
    try:
        places = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("downloaded places are not valid JSON") from exc

    validate(places)
    places.sort(key=lambda place: place["id"])
    pretty = json.dumps(places, indent=2, ensure_ascii=False).encode("utf-8") + b"\n"

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

    print(f"Bundled {len(places)} places into {OUTPUT_FILE.relative_to(APP_DIR.parent)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
