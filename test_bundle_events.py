#!/usr/bin/env python3
"""Unit tests for the bundled-events snapshot validator.

Run from the repository root:

    python3 -m unittest test_bundle_events
"""

import unittest

import bundle_events


def event(**overrides):
    base = {
        "id": 1,
        "lat": 18.788,
        "lon": 99.0156,
        "name": "Bitcoin Half Marathon",
        "starts_at": "2026-11-01T09:00:00+07:00",
        "updated_at": "2026-01-02T00:00:00Z",
    }
    base.update(overrides)
    return base


class ValidateTest(unittest.TestCase):
    def test_accepts_well_formed_events(self):
        bundle_events.validate([
            event(),
            event(id=2, area_id=None, ends_at=None, website=None),
        ])

    def test_accepts_full_field_set(self):
        bundle_events.validate([
            event(
                id=7,
                area_id=42,
                website="https://www.bitcoinmarathon.org/",
                ends_at="2026-11-02T23:00:00+07:00",
            ),
        ])

    def test_rejects_non_list(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate({"id": 1})

    def test_rejects_empty(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([])

    def test_rejects_non_object_entry(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate(["nope"])

    def test_rejects_missing_required_field(self):
        for field in bundle_events.REQUIRED_FIELDS:
            with self.subTest(field=field):
                candidate = event()
                del candidate[field]
                with self.assertRaises(RuntimeError):
                    bundle_events.validate([candidate])

    def test_rejects_non_integer_id(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(id="1")])

    def test_rejects_boolean_id(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(id=True)])

    def test_rejects_non_numeric_coordinates(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(lat="18.0")])
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(lon=False)])

    def test_rejects_coordinates_out_of_range(self):
        for lat, lon in ((90.1, 0.0), (-90.1, 0.0), (0.0, 180.1), (0.0, -180.1)):
            with self.subTest(lat=lat, lon=lon):
                with self.assertRaises(RuntimeError):
                    bundle_events.validate([event(lat=lat, lon=lon)])

    def test_rejects_non_string_or_empty_name(self):
        for name in (5, ""):
            with self.subTest(name=name):
                with self.assertRaises(RuntimeError):
                    bundle_events.validate([event(name=name)])

    def test_rejects_non_integer_area_id(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(area_id="42")])

    def test_rejects_non_string_website(self):
        with self.assertRaises(RuntimeError):
            bundle_events.validate([event(website=5)])

    def test_rejects_invalid_timestamps(self):
        for field in ("starts_at", "ends_at", "updated_at"):
            for value in ("", "not-a-date", 5):
                with self.subTest(field=field, value=value):
                    with self.assertRaises(RuntimeError):
                        bundle_events.validate([event(**{field: value})])


if __name__ == "__main__":
    unittest.main()
