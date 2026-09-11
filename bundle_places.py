#!/usr/bin/env python3
"""Download the latest places snapshot as a bundled Android asset.

Fetches every place from the BTC Map API and writes the raw JSON response to
``app/src/main/assets/bundled-places.json`` so the app can offer an offline
fallback when the network is unavailable.

Run:

    python3 bundle_places.py

The latest snapshot is always fetched, replacing any existing asset.
"""

import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

API_URL = (
    "https://api.btcmap.org/v4/places"
    "?fields=id,lat,lon,icon,name,comments,boosted_until"
)
PROJECT_ROOT = Path(__file__).resolve().parent
APP_DIR = PROJECT_ROOT / "app"
OUTPUT_FILE = APP_DIR / "src" / "main" / "assets" / "bundled-places.json"


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


def main() -> int:
    raw = fetch(API_URL)
    try:
        places = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("downloaded places are not valid JSON") from exc

    if not isinstance(places, list) or not places:
        raise RuntimeError("downloaded places are empty or not a JSON array")

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    # Write atomically so a failed download never leaves a truncated asset.
    tmp_file = OUTPUT_FILE.with_name(OUTPUT_FILE.name + ".tmp")
    tmp_file.write_bytes(raw)
    tmp_file.replace(OUTPUT_FILE)

    print(f"Bundled {len(places)} places into {OUTPUT_FILE.relative_to(APP_DIR.parent)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
