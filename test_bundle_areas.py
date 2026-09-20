#!/usr/bin/env python3
"""Unit tests for the bundled-areas snapshot validator.

Run from the repository root:

    python3 -m unittest test_bundle_areas
"""

import unittest

import bundle_areas


def area(**overrides):
    base = {
        "id": 1,
        "name": "Community",
        "type": "community",
        "url_alias": "community",
        "website_url": "https://btcmap.org/community/community",
        "updated_at": "2026-01-01T00:00:00Z",
    }
    base.update(overrides)
    return base


class ValidateTest(unittest.TestCase):
    def test_accepts_well_formed_areas(self):
        bundle_areas.validate([
            area(),
            area(id=2, icon=None, icon_wide=None, description=None),
            area(id=3, bbox=None, geo_json=None, description=""),
        ])

    def test_accepts_full_field_set(self):
        bundle_areas.validate([
            area(
                icon="https://static.btcmap.org/images/communities/community.jpg",
                icon_wide="https://static.btcmap.org/images/communities/community-wide.jpg",
                description="A community",
                bbox=[-8.72, 42.325, -8.535, 42.535],
                geo_json={"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 0]]]},
            ),
        ])

    def test_rejects_non_list(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate({"id": 1})

    def test_rejects_empty(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate([])

    def test_rejects_non_object_entry(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate(["nope"])

    def test_rejects_missing_required_field(self):
        for field in bundle_areas.REQUIRED_FIELDS:
            with self.subTest(field=field):
                candidate = area()
                del candidate[field]
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([candidate])

    def test_rejects_non_integer_id(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate([area(id="1")])

    def test_rejects_boolean_id(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate([area(id=True)])

    def test_rejects_non_string_required_field(self):
        for field in ("name", "type", "url_alias", "website_url"):
            with self.subTest(field=field):
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([area(**{field: 5})])

    def test_rejects_empty_required_field(self):
        # The planet area legitimately has an empty name, so only the
        # identifiers must be non-empty.
        for field in ("type", "url_alias", "website_url"):
            with self.subTest(field=field):
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([area(**{field: ""})])

    def test_accepts_empty_name(self):
        bundle_areas.validate([area(name="")])

    def test_rejects_invalid_updated_at(self):
        for value in (None, "", "not-a-date", 5):
            with self.subTest(updated_at=value):
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([area(updated_at=value)])

    def test_rejects_non_string_optional_scalar_field(self):
        for field in bundle_areas.OPTIONAL_STRING_FIELDS:
            with self.subTest(field=field):
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([area(**{field: 5})])

    def test_rejects_malformed_bbox(self):
        for bbox in ("1,2,3,4", [1.0, 2.0, 3.0], [1.0, 2.0, 3.0, 4.0, 5.0], [1, 2, 3, True]):
            with self.subTest(bbox=bbox):
                with self.assertRaises(RuntimeError):
                    bundle_areas.validate([area(bbox=bbox)])

    def test_rejects_non_object_geo_json(self):
        with self.assertRaises(RuntimeError):
            bundle_areas.validate([area(geo_json="Polygon")])


if __name__ == "__main__":
    unittest.main()
