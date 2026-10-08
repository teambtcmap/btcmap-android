package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.btcmap.ui.MATERIAL_SYMBOL_NAMES

/**
 * The marker glyph resolution, ported from the app's instrumented `MarkerIconTest`
 * when the Views marker builder went with the map swap. The shared factory
 * measures a place's icon name against the bundled font and falls back for a
 * name the font cannot draw.
 */
class MarkerGlyphTest {

    private val density = Density(2f)
    private val measurer = TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr)

    @Test
    fun recognizesGlyphsInTheBundledFont() {
        val factory = factory(loadIconFont())
        assertEquals("menu_book", factory.renderableGlyph("menu_book"))
        assertEquals("storefront", factory.renderableGlyph("storefront"))
        assertEquals("wc", factory.renderableGlyph("wc"))
    }

    @Test
    fun fallsBackForAGlyphMissingFromTheFont() {
        val factory = factory(loadIconFont())
        assertEquals("storefront", factory.renderableGlyph("foo_bar"))
    }

    @Test
    fun withoutAFontThereIsNoGlyph() {
        val factory = factory(null)
        assertNull(factory.renderableGlyph("storefront"))
    }

    @Test
    fun everyIconNameTheSearchOffersIsRenderable() {
        // A name the font has no ligature for measures as its literal letters
        // and resolves to the fallback: exactly the broken icons the search
        // must never offer.
        val factory = factory(loadIconFont())
        val broken = MATERIAL_SYMBOL_NAMES.filter { factory.renderableGlyph(it) != it }
        assertEquals(emptyList(), broken)
    }

    private fun factory(iconFont: FontFamily?) = MarkerBitmapFactory(
        textMeasurer = measurer,
        iconFont = iconFont,
        density = density,
        palette = MarkerPalette(
            markerBackground = Color(0xFFF7931A),
            markerIcon = Color.White,
            boostedMarkerBackground = Color(0xFF7B3FE4),
            boostedMarkerIcon = Color.White,
            badgeBackground = Color(0xFFE53935),
            badgeText = Color.White,
        ),
    )

    /** The newest bundled Material Symbols font, put on the classpath by the build. */
    private fun loadIconFont(): FontFamily {
        val dir = File(System.getProperty("btcmap.iconFontDir") ?: "app/src/main/assets")
        val file = dir.listFiles { candidate -> candidate.name.startsWith("material-symbols") }
            ?.maxByOrNull { it.name }
            ?: error("icon font not found in $dir")
        return FontFamily(Font(file))
    }
}
