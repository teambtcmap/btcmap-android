#!/usr/bin/env python3
"""Unit tests for the Material Symbols icon-name bundler.

Run from the repository root:

    python3 -m unittest test_bundle_icons
"""

import unittest

import bundle_icons


class ExtractNamesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.names = bundle_icons.extract_names(bundle_icons.newest_font())

    def test_contains_names_the_app_uses(self):
        for name in (
            "notes",
            "star",
            "favorite",
            "local_atm",
            "storefront",
            "currency_exchange",
        ):
            self.assertIn(name, self.names)

    def test_reconstructsNamesWithDigits(self):
        # A ligature's components are glyph names, so digits must be mapped back
        # to characters; otherwise "add_2" becomes the unrenderable
        # "add_digit_two".
        for name in ("add_2", "360", "3d_rotation", "10k", "signal_cellular_4_bar"):
            self.assertIn(name, self.names)

    def test_keepsNoGlyphNames(self):
        for name in self.names:
            self.assertNotIn("digit_", name)
            self.assertNotIn("underscore", name)

    def test_is_sorted_and_unique(self):
        self.assertEqual(self.names, sorted(self.names))
        self.assertEqual(len(self.names), len(set(self.names)))

    def test_is_substantial(self):
        self.assertGreater(len(self.names), 3000)


class RenderKotlinTest(unittest.TestCase):
    def test_chunks_large_lists(self):
        names = [f"icon_{index}" for index in range(bundle_icons.CHUNK_SIZE + 1)]
        source = bundle_icons.render_kotlin(names, "font.ttf")
        self.assertIn("materialSymbolNamesChunk0()", source)
        self.assertIn("materialSymbolNamesChunk1()", source)
        self.assertIn('"icon_0"', source)
        self.assertIn(f'"icon_{bundle_icons.CHUNK_SIZE}"', source)


if __name__ == "__main__":
    unittest.main()
