#!/usr/bin/env python3
"""Download the latest place-comments snapshot as a bundled Android asset.

Fetches every comment from the BTC Map API and writes it to
``app/src/main/assets/bundled-comments.json``. Comments are effectively static
(a few thousand accrue over years), so the snapshot carries each comment's real
``updated_at`` and the first sync only has to fetch the handful that changed
since the snapshot was generated. A place's comments are then readable offline.

``deleted_at`` is deliberately not requested: requesting it also makes the API
return soft-deleted tombstones, which the bundle does not need because a comment
that was deleted before the snapshot was built is simply absent from it.

The output is pretty-printed and sorted by id. This keeps the diff of a
refresh limited to the comments that actually changed instead of rewriting the
whole file.

Run:

    python3 bundle_comments.py

The latest snapshot is always fetched, replacing any existing asset.
"""

import datetime
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

# ``GET /v4/place-comments`` always returns this shape; it has no ``fields``
# projection. Keep the names in sync with ``toGetCommentsItems`` in
# CommentApi.kt and ``readBundledComment`` in BundledComments.kt.
API_URL = "https://api.btcmap.org/v4/place-comments"
PROJECT_ROOT = Path(__file__).resolve().parent
APP_DIR = PROJECT_ROOT / "app"
OUTPUT_FILE = APP_DIR / "src" / "main" / "assets" / "bundled-comments.json"

REQUIRED_FIELDS = ("id", "place_id", "text", "created_at", "updated_at")


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


def _is_timestamp(value: object) -> bool:
    if not isinstance(value, str) or not value:
        return False
    try:
        datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return False
    return True


def validate(comments: list) -> None:
    """Reject an unexpected shape with a clear message instead of a traceback.

    Field presence and types are checked here so a bad download fails at
    bundling time, rather than on-device where a malformed snapshot forces the
    importer to roll back the whole seed.
    """
    if not isinstance(comments, list):
        raise RuntimeError("downloaded comments are not a JSON array")
    if not comments:
        raise RuntimeError("downloaded comments are empty")
    for index, comment in enumerate(comments):
        if not isinstance(comment, dict):
            raise RuntimeError(f"comment at index {index} is not a JSON object")
        for field in REQUIRED_FIELDS:
            if field not in comment:
                raise RuntimeError(f"comment at index {index} is missing '{field}'")

        comment_id = comment["id"]
        if not isinstance(comment_id, int) or isinstance(comment_id, bool):
            raise RuntimeError(f"comment at index {index} has a non-integer 'id'")

        place_id = comment["place_id"]
        if not isinstance(place_id, int) or isinstance(place_id, bool):
            raise RuntimeError(f"comment {comment_id} has a non-integer 'place_id'")

        text = comment["text"]
        if not isinstance(text, str) or not text:
            raise RuntimeError(f"comment {comment_id} has a non-string or empty 'text'")

        # The seed's whole point is the delta sync, which pages from the newest
        # stored ``updated_at``: an unparseable one would corrupt that cursor.
        for field in ("created_at", "updated_at"):
            if not _is_timestamp(comment[field]):
                raise RuntimeError(f"comment {comment_id} has an invalid '{field}'")


def main() -> int:
    raw = fetch(API_URL)
    try:
        comments = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise RuntimeError("downloaded comments are not valid JSON") from exc

    validate(comments)
    comments.sort(key=lambda comment: comment["id"])
    pretty = json.dumps(comments, indent=2, ensure_ascii=False).encode("utf-8") + b"\n"

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

    print(f"Bundled {len(comments)} comments into {OUTPUT_FILE.relative_to(APP_DIR.parent)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
