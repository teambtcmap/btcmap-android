#!/usr/bin/env python3
"""Download the latest events snapshot as a bundled Android asset.

Fetches every event from the BTC Map API, keeps only the upcoming ones and
writes them to ``app/src/main/assets/bundled-events.json``. The snapshot carries
each event's real ``updated_at``, so the first events sync only fetches the few
that changed since the snapshot was generated, and events are searchable
offline.

Unlike places, areas and comments, ``updated_since`` is required: without it
the endpoint falls back to a legacy full snapshot that omits ``updated_at`` and
so cannot seed a delta cursor. ``deleted_at`` is deliberately not requested:
requesting it (or ``include_deleted``) also makes the API return soft-deleted
tombstones, which the bundle does not need because an event that was deleted
before the snapshot was built is simply absent from it.

Delta mode deliberately returns events that have already started, so a sync
client can observe edits and deletions of them, and an event without a real
start date is stored at the epoch. Every screen filters those out at display
time, so they are dropped here instead of shipping rows the app never shows.

The output is pretty-printed and sorted by id. This keeps the diff of a
refresh limited to the events that actually changed instead of rewriting the
whole file.

Run:

    python3 bundle_events.py

The latest snapshot is always fetched, replacing any existing asset.
"""

import datetime
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

# ``updated_since`` is not optional: see the module docstring. Keep this in sync
# with ``getEvents`` in EventApi.kt and ``readBundledEvent`` in BundledEvents.kt.
API_URL = "https://api.btcmap.org/v4/events?updated_since=1970-01-01T00:00:00Z"
PROJECT_ROOT = Path(__file__).resolve().parent
APP_DIR = PROJECT_ROOT / "app"
OUTPUT_FILE = APP_DIR / "src" / "main" / "assets" / "bundled-events.json"

REQUIRED_FIELDS = ("id", "lat", "lon", "name", "starts_at", "updated_at")


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
    # bool is a subclass of int, but a boolean coordinate is a bug.
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _parse_timestamp(value: str) -> datetime.datetime:
    timestamp = datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    # The app parses these with ZonedDateTime.parse, which rejects a value
    # without an offset; match that here so a naive timestamp fails validation
    # instead of raising a TypeError when it is compared against UTC now.
    if timestamp.tzinfo is None:
        raise ValueError("timestamp has no UTC offset")
    return timestamp


def _is_timestamp(value: object) -> bool:
    if not isinstance(value, str) or not value:
        return False
    try:
        _parse_timestamp(value)
    except ValueError:
        return False
    return True


def upcoming(events: list, now: datetime.datetime) -> list:
    """Keep only the events whose start is strictly after [now].

    Mirrors the app's ``ZonedDateTime.isUpcoming`` rule: the delta endpoint
    returns past events and the epoch placeholder, and the screens filter them
    out anyway, so they are not worth bundling.
    """
    return [event for event in events if _parse_timestamp(event["starts_at"]) > now]


def validate(events: list) -> None:
    """Reject an unexpected shape with a clear message instead of a traceback.

    Field presence, types and coordinate ranges are checked here so a bad
    download fails at bundling time, rather than on-device where a malformed
    snapshot forces the importer to roll back the whole seed.
    """
    if not isinstance(events, list):
        raise RuntimeError("downloaded events are not a JSON array")
    if not events:
        raise RuntimeError("downloaded events are empty")
    for index, event in enumerate(events):
        if not isinstance(event, dict):
            raise RuntimeError(f"event at index {index} is not a JSON object")
        for field in REQUIRED_FIELDS:
            if field not in event:
                raise RuntimeError(f"event at index {index} is missing '{field}'")

        event_id = event["id"]
        if not isinstance(event_id, int) or isinstance(event_id, bool):
            raise RuntimeError(f"event at index {index} has a non-integer 'id'")

        lat = event["lat"]
        lon = event["lon"]
        if not _is_number(lat):
            raise RuntimeError(f"event {event_id} has a non-numeric 'lat'")
        if not _is_number(lon):
            raise RuntimeError(f"event {event_id} has a non-numeric 'lon'")
        if not -90 <= lat <= 90:
            raise RuntimeError(f"event {event_id} has a 'lat' outside [-90, 90]")
        if not -180 <= lon <= 180:
            raise RuntimeError(f"event {event_id} has a 'lon' outside [-180, 180]")

        name = event["name"]
        if not isinstance(name, str) or not name:
            raise RuntimeError(f"event {event_id} has a non-string or empty 'name'")

        area_id = event.get("area_id")
        if area_id is not None and (not isinstance(area_id, int) or isinstance(area_id, bool)):
            raise RuntimeError(f"event {event_id} has a non-integer 'area_id'")

        website = event.get("website")
        if website is not None and not isinstance(website, str):
            raise RuntimeError(f"event {event_id} has a non-string 'website'")

        # The seed's whole point is the delta sync, which pages from the newest
        # stored ``updated_at``: an unparseable one would corrupt that cursor.
        for field in ("starts_at", "ends_at", "updated_at"):
            value = event.get(field)
            if value is not None and not _is_timestamp(value):
                raise RuntimeError(f"event {event_id} has an invalid '{field}'")


def main() -> int:
    raw = fetch(API_URL)
    try:
        events = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("downloaded events are not valid JSON") from exc

    validate(events)

    # Delta mode returns already-started events and the epoch placeholder; keep
    # only the upcoming ones, matching what every screen in the app displays.
    events = upcoming(events, datetime.datetime.now(datetime.timezone.utc))
    if not events:
        raise RuntimeError("the API returned no upcoming events")

    events.sort(key=lambda event: event["id"])
    pretty = json.dumps(events, indent=2, ensure_ascii=False).encode("utf-8") + b"\n"

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

    location = OUTPUT_FILE.relative_to(APP_DIR.parent)
    print(f"Bundled {len(events)} upcoming events into {location}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
