#!/usr/bin/env python3
"""Unit tests for the bundled-comments snapshot validator.

Run from the repository root:

    python3 -m unittest test_bundle_comments
"""

import unittest

import bundle_comments


def comment(**overrides):
    base = {
        "id": 1,
        "place_id": 42,
        "text": "Bitcoin accepted here",
        "created_at": "2026-01-01T00:00:00Z",
        "updated_at": "2026-01-02T00:00:00Z",
    }
    base.update(overrides)
    return base


class ValidateTest(unittest.TestCase):
    def test_accepts_well_formed_comments(self):
        bundle_comments.validate([
            comment(),
            comment(id=2, place_id=7, text="Paid in sats"),
        ])

    def test_rejects_non_list(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate({"id": 1})

    def test_rejects_empty(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([])

    def test_rejects_non_object_entry(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate(["nope"])

    def test_rejects_missing_required_field(self):
        for field in bundle_comments.REQUIRED_FIELDS:
            with self.subTest(field=field):
                candidate = comment()
                del candidate[field]
                with self.assertRaises(RuntimeError):
                    bundle_comments.validate([candidate])

    def test_rejects_non_integer_id(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([comment(id="1")])

    def test_rejects_boolean_id(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([comment(id=True)])

    def test_rejects_non_integer_place_id(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([comment(place_id="42")])

    def test_rejects_non_string_text(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([comment(text=5)])

    def test_rejects_empty_text(self):
        with self.assertRaises(RuntimeError):
            bundle_comments.validate([comment(text="")])

    def test_rejects_invalid_timestamps(self):
        for field in ("created_at", "updated_at"):
            for value in (None, "", "not-a-date", 5):
                with self.subTest(field=field, value=value):
                    with self.assertRaises(RuntimeError):
                        bundle_comments.validate([comment(**{field: value})])


if __name__ == "__main__":
    unittest.main()
