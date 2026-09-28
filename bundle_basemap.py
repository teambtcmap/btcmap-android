#!/usr/bin/env python3
"""Bundle a low-zoom OpenFreeMap basemap as a single PMTiles asset.

The app ships a whole-world basemap for the low zoom levels so the map still
renders when offline. The tiles are fetched from OpenFreeMap's planet tileset
and written as one PMTiles v3 archive to
``app/src/main/assets/basemap.pmtiles``. MapLibre reads a PMTiles archive over
its own ``pmtiles://`` scheme, so no per-tile asset tree is needed.

Only zoom levels 0 through ``MAX_ZOOM`` are bundled. Above that the styles keep
their hosted source, so street-level detail is unaffected when online; see
``app/src/main/kotlin/org/btcmap/map/BundledBasemapStyle.kt``.

The tile URL template and the ``vector_layers`` metadata are taken from the
tileset's TileJSON, so a new OpenFreeMap planet build is picked up on the next
run without a code change.

PMTiles v3 is written here in pure Python (the bundlers have no third-party
dependencies): see https://github.com/protomaps/PMTiles/blob/main/spec/v3/spec.md

Run:

    python3 bundle_basemap.py

The archive is always rebuilt, replacing any existing asset.
"""

import concurrent.futures
import gzip
import json
import struct
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path

TILEJSON_URL = "https://tiles.openfreemap.org/planet"

# The highest zoom level bundled. Together with zoom 0 this is the archive's
# zoom range; the styles' hosted source covers everything above it.
MAX_ZOOM = 4

PROJECT_ROOT = Path(__file__).resolve().parent
ASSETS_ROOT = PROJECT_ROOT / "app" / "src" / "main" / "assets"
OUTPUT_FILE = ASSETS_ROOT / "basemap.pmtiles"

TMP_SUFFIX = ".tmp"
USER_AGENT = "btcmap-android-bundler/1.0"

# PMTiles enum values used by the header.
COMPRESSION_NONE = 1
COMPRESSION_GZIP = 2
TILE_TYPE_MVT = 1

# Whole world in Web Mercator.
MIN_LAT = -85.05112878
MAX_LAT = 85.05112878

# Fetches per run; low-zoom tiles are few, so this only bounds a burst.
FETCH_WORKERS = 16


@dataclass
class Tile:
    z: int
    x: int
    y: int
    data: bytes

    @property
    def tile_id(self) -> int:
        return zxy_to_tile_id(self.z, self.x, self.y)


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def zxy_to_tile_id(z: int, x: int, y: int) -> int:
    """The PMTiles v3 TileID: a Hilbert curve index, cumulative over zooms.

    Tiles of all levels below ``z`` come first (the "addressed tiles" base),
    then the Hilbert index of (x, y) within level ``z``.
    """
    acc = sum(4**t for t in range(z))
    n = 1 << z
    d = 0
    s = n >> 1
    tx, ty = x, y
    while s > 0:
        rx = 1 if tx & s else 0
        ry = 1 if ty & s else 0
        d += s * s * ((3 * rx) ^ ry)
        if ry == 0:
            if rx == 1:
                tx = n - 1 - tx
                ty = n - 1 - ty
            tx, ty = ty, tx
        s >>= 1
    return acc + d


def _check_tile_ids() -> None:
    """Assert the Hilbert mapping against the examples in the specification."""
    expected = {
        (0, 0, 0): 0,
        (1, 0, 0): 1,
        (1, 0, 1): 2,
        (1, 1, 1): 3,
        (1, 1, 0): 4,
        (2, 0, 0): 5,
        (12, 3423, 1763): 19078479,
    }
    for (z, x, y), tile_id in expected.items():
        got = zxy_to_tile_id(z, x, y)
        if got != tile_id:
            raise RuntimeError(f"tile id for z{z}/{x}/{y} is {got}, expected {tile_id}")


def write_varint(buffer: bytearray, value: int) -> None:
    """Little-endian base-128 varint, as PMTiles directories use."""
    if value < 0:
        raise ValueError("varint cannot be negative")
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            buffer.append(byte | 0x80)
        else:
            buffer.append(byte)
            return


def encode_directory(entries: list[tuple[int, int, int, int]]) -> bytes:
    """Encode a PMTiles directory from (tile_id, offset, length, run_length).

    Entries must already be sorted by tile id; offsets are relative to the tile
    data section, and a contiguous offset is encoded as 0. The result is
    compressed according to the header's internal compression.
    """
    buffer = bytearray()
    write_varint(buffer, len(entries))

    last_id = 0
    for tile_id, _, _, _ in entries:
        write_varint(buffer, tile_id - last_id)
        last_id = tile_id

    for _, _, _, run_length in entries:
        write_varint(buffer, run_length)

    for _, _, length, _ in entries:
        write_varint(buffer, length)

    next_byte = 0
    for index, (_, offset, length, _) in enumerate(entries):
        if index > 0 and offset == next_byte:
            write_varint(buffer, 0)
        else:
            write_varint(buffer, offset + 1)
        next_byte = offset + length

    return gzip.compress(bytes(buffer), mtime=0)


def encode_header(
    root_offset: int,
    root_length: int,
    metadata_offset: int,
    metadata_length: int,
    tile_data_offset: int,
    tile_data_length: int,
    addressed_tiles: int,
    tile_entries: int,
    tile_contents: int,
) -> bytes:
    def position(longitude: float, latitude: float) -> bytes:
        return struct.pack("<ii", round(longitude * 1e7), round(latitude * 1e7))

    header = struct.pack(
        "<7sBQQQQQQQQQQQBBBBBBiiiiBii",
        b"PMTiles",
        3,
        root_offset,
        root_length,
        metadata_offset,
        metadata_length,
        # No leaf directories: the whole low-zoom directory fits in the root.
        metadata_offset + metadata_length,
        0,
        tile_data_offset,
        tile_data_length,
        addressed_tiles,
        tile_entries,
        tile_contents,
        # clustered, internal compression, tile compression, tile type
        1,
        COMPRESSION_GZIP,
        COMPRESSION_NONE,
        TILE_TYPE_MVT,
        0,
        MAX_ZOOM,
        *struct.unpack("<iiii", position(-180.0, MIN_LAT) + position(180.0, MAX_LAT)),
        0,
        *struct.unpack("<ii", position(0.0, 0.0)),
    )
    if len(header) != 127:
        raise RuntimeError(f"header is {len(header)} bytes, expected 127")
    return header


def build_metadata(tilejson: dict, tile_count: int) -> bytes:
    metadata = {
        "name": f"OpenFreeMap planet low zoom (z0-z{MAX_ZOOM}), bundled with BTC Map",
        "attribution": (
            '<a href="https://openfreemap.org/" target="_blank">OpenFreeMap</a> '
            '© <a href="https://www.openstreetmap.org/copyright" target="_blank">'
            "OpenStreetMap</a> contributors"
        ),
        "type": "baselayer",
        "version": "1",
        "vector_layers": tilejson.get("vector_layers", []),
        "bundled_tiles": tile_count,
    }
    return gzip.compress(json.dumps(metadata, ensure_ascii=False).encode("utf-8"), mtime=0)


def build_archive(tiles: list[Tile], tilejson: dict) -> bytes:
    """Assemble the five PMTiles sections into the archive bytes."""
    ordered = sorted(tiles, key=lambda tile: tile.tile_id)

    entries: list[tuple[int, int, int, int]] = []
    tile_data = bytearray()
    for tile in ordered:
        offset = len(tile_data)
        tile_data += tile.data
        entries.append((tile.tile_id, offset, len(tile.data), 1))

    root_directory = encode_directory(entries)
    metadata = build_metadata(tilejson, len(ordered))

    root_offset = 127
    metadata_offset = root_offset + len(root_directory)
    tile_data_offset = metadata_offset + len(metadata)

    header = encode_header(
        root_offset=root_offset,
        root_length=len(root_directory),
        metadata_offset=metadata_offset,
        metadata_length=len(metadata),
        tile_data_offset=tile_data_offset,
        tile_data_length=len(tile_data),
        addressed_tiles=len(ordered),
        tile_entries=len(ordered),
        tile_contents=len(ordered),
    )

    return header + root_directory + metadata + bytes(tile_data)


def collect_tiles(template: str) -> list[Tile]:
    coordinates = [
        (z, x, y)
        for z in range(MAX_ZOOM + 1)
        for x in range(1 << z)
        for y in range(1 << z)
    ]

    def load(coordinate: tuple[int, int, int]) -> Tile | None:
        z, x, y = coordinate
        try:
            return Tile(z, x, y, fetch(template.format(z=z, x=x, y=y)))
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None
            raise

    with concurrent.futures.ThreadPoolExecutor(max_workers=FETCH_WORKERS) as pool:
        tiles = [tile for tile in pool.map(load, coordinates) if tile is not None]

    if not tiles:
        raise RuntimeError("no tiles were downloaded")
    return tiles


def verify(archive: bytes, tiles: list[Tile]) -> None:
    """Read the archive back and confirm the header and every tile entry."""
    if archive[:7] != b"PMTiles":
        raise RuntimeError("archive does not start with the PMTiles magic")
    if archive[7] != 3:
        raise RuntimeError("archive is not PMTiles version 3")

    (
        root_offset,
        root_length,
        metadata_offset,
        metadata_length,
        leaf_offset,
        leaf_length,
        tile_data_offset,
        tile_data_length,
    ) = struct.unpack_from("<QQQQQQQQ", archive, 8)

    if leaf_length != 0:
        raise RuntimeError("bundled archive should not have leaf directories")
    if root_offset + root_length > 16384:
        raise RuntimeError("root directory does not fit in the first 16 KiB")
    if metadata_offset + metadata_length != leaf_offset:
        raise RuntimeError("metadata section is not where the header says")

    # Decode the directory using the inverse of write_varint and check that
    # every expected tile is present and points at its own bytes.
    directory = gzip.decompress(archive[root_offset : root_offset + root_length])
    entries = _decode_directory(directory)

    by_id = {tile.tile_id: tile for tile in tiles}
    for tile_id, offset, length, _ in entries:
        tile = by_id.get(tile_id)
        if tile is None:
            raise RuntimeError(f"archive contains an unexpected tile id {tile_id}")
        start = tile_data_offset + offset
        if archive[start : start + length] != tile.data:
            raise RuntimeError(f"tile data mismatch for id {tile_id}")
    if len(entries) != len(tiles):
        raise RuntimeError(f"archive has {len(entries)} entries for {len(tiles)} tiles")


def _read_varint(buffer: bytes, position: int) -> tuple[int, int]:
    value = 0
    shift = 0
    while True:
        byte = buffer[position]
        position += 1
        value |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return value, position
        shift += 7


def _decode_directory(buffer: bytes) -> list[tuple[int, int, int, int]]:
    position = 0
    n, position = _read_varint(buffer, position)
    tile_ids = []
    last = 0
    for _ in range(n):
        delta, position = _read_varint(buffer, position)
        last += delta
        tile_ids.append(last)

    run_lengths = []
    for _ in range(n):
        value, position = _read_varint(buffer, position)
        run_lengths.append(value)

    lengths = []
    for _ in range(n):
        value, position = _read_varint(buffer, position)
        lengths.append(value)

    offsets = []
    next_byte = 0
    for index in range(n):
        value, position = _read_varint(buffer, position)
        if index > 0 and value == 0:
            offset = next_byte
        else:
            offset = value - 1
        offsets.append(offset)
        next_byte = offset + lengths[index]

    return list(zip(tile_ids, offsets, lengths, run_lengths))


def main() -> int:
    _check_tile_ids()

    print(f"[tilejson] {TILEJSON_URL}")
    tilejson = json.loads(fetch(TILEJSON_URL))
    templates = tilejson.get("tiles") or []
    if not templates:
        raise RuntimeError("the tileset does not declare a tile URL template")
    template = templates[0]

    print(f"[tiles] downloading z0-z{MAX_ZOOM} from {template}")
    tiles = collect_tiles(template)
    total = sum(len(tile.data) for tile in tiles)
    print(f"  downloaded {len(tiles)} tiles, {total / 1e6:.1f} MB")

    archive = build_archive(tiles, tilejson)
    verify(archive, tiles)

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    tmp_file = OUTPUT_FILE.with_name(OUTPUT_FILE.name + TMP_SUFFIX)
    try:
        tmp_file.write_bytes(archive)
        tmp_file.replace(OUTPUT_FILE)
    except BaseException:
        tmp_file.unlink(missing_ok=True)
        raise

    location = OUTPUT_FILE.relative_to(PROJECT_ROOT)
    print(f"Wrote {location} ({len(archive) / 1e6:.1f} MB)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (urllib.error.URLError, OSError, ValueError, RuntimeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        sys.exit(1)
