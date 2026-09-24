#!/usr/bin/env python3
"""Unit tests for the bundled-events snapshot validator.

Run from the repository root:

    python3 -m unittest test_bundle_events
"""

import datetime
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
            for value in ("", "not-a-date", "2026-11-01T09:00:00", 5):
                with self.subTest(field=field, value=value):
                    with self.assertRaises(RuntimeError):
                        bundle_events.validate([event(**{field: value})])


class UpcomingTest(unittest.TestCase):
    NOW = datetime.datetime(2026, 10, 1, 0, 0, tzinfo=datetime.timezone.utc)

    def test_keeps_only_events_that_have_not_started(self):
        future = event(id=1, starts_at="2026-10-02T00:00:00Z")
        past = event(id=2, starts_at="2026-09-30T00:00:00Z")
        epoch = event(id=3, starts_at="1970-01-01T00:00:00Z")

        self.assertEqual(
            [future],
            bundle_events.upcoming([past, future, epoch], self.NOW),
        )

    def test_event_starting_exactly_now_is_dropped(self):
        self.assertEqual(
            [],
            bundle_events.upcoming([event(starts_at="2026-10-01T00:00:00Z")], self.NOW),
        )

    def test_offset_start_is_compared_as_an_instant(self):
        # 2026-11-01T09:00:00+07:00 is 2026-11-01T02:00:00Z (future);
        # 2026-09-30T20:00:00+07:00 is 2026-09-30T13:00:00Z (past).
        later = event(id=1, starts_at="2026-11-01T09:00:00+07:00")
        earlier = event(id=2, starts_at="2026-09-30T20:00:00+07:00")

        self.assertEqual([later], bundle_events.upcoming([later, earlier], self.NOW))

    def test_empty_input_yields_empty(self):
        self.assertEqual([], bundle_events.upcoming([], self.NOW))


if __name__ == "__main__":
    unittest.main()
