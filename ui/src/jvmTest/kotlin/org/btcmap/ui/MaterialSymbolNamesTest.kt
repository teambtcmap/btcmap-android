package org.btcmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The generated icon-name list the add-note search reads. */
class MaterialSymbolNamesTest {

    @Test
    fun containsTheIconsTheAppUses() {
        listOf(
            "notes",
            "star",
            "favorite",
            "local_atm",
            "storefront",
            "currency_exchange",
            "event",
            // The profile screen's section and navigation icons.
            "person",
            "lock",
            "bookmark",
            "public",
            "photo_library",
            "logout",
        ).forEach { assertTrue(it in MATERIAL_SYMBOL_NAMES, "missing icon: $it") }
    }

    @Test
    fun reconstructsNamesWithDigits() {
        // The font names a ligature's components by glyph, so a digit must be
        // mapped back to its character; "add_digit_two" would not render.
        listOf("add_2", "360", "3d_rotation", "10k", "signal_cellular_4_bar")
            .forEach { assertTrue(it in MATERIAL_SYMBOL_NAMES, "missing icon: $it") }
    }

    @Test
    fun keepsNoGlyphNames() {
        MATERIAL_SYMBOL_NAMES.forEach { name ->
            assertTrue("digit_" !in name, "glyph name leaked into: $name")
            assertTrue("underscore" !in name, "glyph name leaked into: $name")
        }
    }

    @Test
    fun isSortedAndUnique() {
        assertEquals(MATERIAL_SYMBOL_NAMES.sorted(), MATERIAL_SYMBOL_NAMES)
        assertEquals(MATERIAL_SYMBOL_NAMES.toSet().size, MATERIAL_SYMBOL_NAMES.size)
    }
}
