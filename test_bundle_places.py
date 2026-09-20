#!/usr/bin/env python3
"""Unit tests for the bundled-places snapshot validator.

Run from the repository root:

    python3 -m unittest test_bundle_places
"""

import unittest

import bundle_places


def place(**overrides):
    base = {
        "id": 1,
        "lat": 1.5,
        "lon": 2.5,
        "icon": "store",
        "updated_at": "2026-01-01T00:00:00Z",
    }
    base.update(overrides)
    return base


class ValidateTest(unittest.TestCase):
    def test_accepts_well_formed_places(self):
        bundle_places.validate([
            place(),
            place(id=2, lat=-90, lon=180, name=None, comments=3, boosted_until=None),
            place(id=3, name="Cafe", comments=0, boosted_until="2026-02-01T00:00:00Z"),
            place(id=4, lat=90, lon=-180),
        ])

    def test_accepts_full_field_set(self):
        bundle_places.validate([
            place(
                name="Cafe",
                localized_name={"en": "Cafe", "de": "Café"},
                updated_at="2026-01-01T00:00:00Z",
                required_app_url="https://example.com",
                boosted_until="2026-02-01T00:00:00Z",
                verified_at="2026-01-01",
                address="1 Main St",
                opening_hours="Mo-Fr 08:00-18:00",
                localized_opening_hours={"en": "Mo-Fr 08:00-18:00"},
                website="https://example.com",
                phone="+1234567890",
                email="a@example.com",
                twitter="https://x.com/example",
                facebook="https://facebook.com/example",
                instagram="https://instagram.com/example",
                line="https://line.me/example",
                comments=2,
                telegram="https://t.me/example",
                osm_id="node:1",
            ),
        ])

    def test_rejects_non_list(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate({"id": 1})

    def test_rejects_empty(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([])

    def test_rejects_non_object_entry(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate(["nope"])

    def test_rejects_missing_required_field(self):
        for field in bundle_places.REQUIRED_FIELDS:
            with self.subTest(field=field):
                candidate = place()
                del candidate[field]
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([candidate])

    def test_rejects_non_integer_id(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(id="1")])

    def test_rejects_boolean_id(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(id=True)])

    def test_rejects_non_numeric_lat(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(lat="1.5")])

    def test_rejects_boolean_lon(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(lon=False)])

    def test_rejects_lat_out_of_range(self):
        for lat in (90.1, -90.1):
            with self.subTest(lat=lat):
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([place(lat=lat)])

    def test_rejects_lon_out_of_range(self):
        for lon in (180.1, -180.1):
            with self.subTest(lon=lon):
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([place(lon=lon)])

    def test_rejects_non_string_icon(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(icon=5)])

    def test_rejects_empty_icon(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(icon="")])

    def test_rejects_non_string_name(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(name=3)])

    def test_rejects_non_integer_comments(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(comments="3")])

    def test_rejects_boolean_comments(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(comments=True)])

    def test_rejects_non_string_boosted_until(self):
        with self.assertRaises(RuntimeError):
            bundle_places.validate([place(boosted_until=5)])

    def test_rejects_invalid_updated_at(self):
        for value in (None, "", "not-a-date", 5):
            with self.subTest(updated_at=value):
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([place(updated_at=value)])

    def test_rejects_non_string_optional_scalar_field(self):
        for field in bundle_places.OPTIONAL_STRING_FIELDS:
            with self.subTest(field=field):
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([place(**{field: 5})])

    def test_rejects_non_object_localized_field(self):
        for field in bundle_places.OPTIONAL_OBJECT_FIELDS:
            with self.subTest(field=field):
                with self.assertRaises(RuntimeError):
                    bundle_places.validate([place(**{field: "en"})])

    def test_accepts_relative_or_malformed_urls(self):
        # The app degrades these to null with HttpUrl.toHttpUrlOrNull(), so the
        # bundler must accept the values the live API actually returns.
        bundle_places.validate([
            place(website="www.example.com", required_app_url="yes", telegram="https:///example"),
        ])

    def test_accepts_null_optional_fields(self):
        bundle_places.validate([
            place(**{field: None for field in bundle_places.OPTIONAL_STRING_FIELDS}),
        ])


if __name__ == "__main__":
    unittest.main()
