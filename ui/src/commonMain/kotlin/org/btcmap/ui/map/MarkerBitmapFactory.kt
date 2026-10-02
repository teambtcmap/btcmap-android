package org.btcmap.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.btcmap.db.table.place.Marker
import org.btcmap.map.MAX_COMMENT_BADGE
import org.btcmap.map.isBoosted
import org.btcmap.map.isOutdated
import org.maplibre.compose.map.ResolvedStyleImage
import org.maplibre.compose.map.StyleImages
import kotlin.math.roundToInt

/**
 * Registers [image] under [name]. Since MapLibre Compose 0.19 the style images
 * take a [ResolvedStyleImage] rather than a bare [ImageBitmap].
 */
internal fun StyleImages.setBitmap(name: String, image: ImageBitmap) {
    set(name, ResolvedStyleImage.fromBitmap(image))
}

/**
 * Builds the marker bitmaps in common Compose graphics, ported from the Views
 * builder that did the same with the Android Canvas. The pin outline is the
 * same vector as `R.drawable.map_marker`; the glyph and the comment badge are
 * laid out at the same dp sizes.
 */
class MarkerBitmapFactory(
    private val textMeasurer: TextMeasurer,
    private val iconFont: FontFamily?,
    private val density: Density,
    private val palette: MarkerPalette,
) {

    /** The marker for a place, with its glyph, variant and comment badge. */
    fun merchantMarker(marker: Marker): ImageBitmap {
        val now = java.time.ZonedDateTime.now()
        return render(
            character = marker.icon,
            outdated = marker.isOutdated(now),
            boosted = marker.isBoosted(now),
            comments = marker.comments.takeIf { it > 0 },
        )
    }

    /**
     * The plain [R.drawable.map_marker] pin in [backgroundColor]. Exchange and
     * event markers draw it on its own, with their glyph on a separate layer.
     */
    fun pin(backgroundColor: Color): ImageBitmap {
        val pinPx = with(density) { PIN_SIZE_DP.dp.toPx() }
        val size = pinPx.roundToInt()
        return draw(size, size) {
            drawPath(scaledPin(pinPx), color = backgroundColor)
        }
    }

    /**
     * The place glyph on its own, for the exchange and event markers, which
     * draw the glyph over the shared pin instead of baking it in.
     */
    fun icon(character: String, color: Color): ImageBitmap {
        val glyph = renderableGlyph(character) ?: return ImageBitmap(1, 1)
        val style = TextStyle(
            fontFamily = iconFont,
            fontSize = with(density) { GLYPH_TEXT_DP.dp.toSp() },
            color = color,
        )
        val measured = textMeasurer.measure(glyph, style)
        val width = measured.size.width + ICON_PADDING_PX * 2
        val height = measured.size.height + ICON_PADDING_PX * 2
        return draw(width, height) {
            drawText(
                textMeasurer = textMeasurer,
                text = glyph,
                topLeft = Offset(ICON_PADDING_PX.toFloat(), ICON_PADDING_PX.toFloat()),
                style = style,
            )
        }
    }

    private fun render(
        character: String?,
        outdated: Boolean,
        boosted: Boolean,
        comments: Long?,
    ): ImageBitmap {
        val boostedAndCurrent = boosted && !outdated
        val pinColor = if (boostedAndCurrent) palette.boostedMarkerBackground else palette.markerBackground
        val glyphColor = when {
            outdated -> palette.markerIcon.copy(alpha = palette.markerIcon.alpha * OUTDATED_ICON_ALPHA)
            boostedAndCurrent -> palette.boostedMarkerIcon
            else -> palette.markerIcon
        }

        val pinPx = with(density) { PIN_SIZE_DP.dp.toPx() }
        val topPadding = if (comments != null) with(density) { BADGE_TOP_PADDING_DP.dp.toPx() } else 0f
        val width = pinPx.roundToInt()
        val height = (pinPx + topPadding).roundToInt()

        return draw(width, height) {
            translate(top = topPadding) {
                drawPath(scaledPin(pinPx), color = pinColor)
            }

            val glyph = renderableGlyph(character)
            if (glyph != null) {
                val style = TextStyle(
                    fontFamily = iconFont,
                    fontSize = with(density) { GLYPH_TEXT_DP.dp.toSp() },
                    color = glyphColor,
                )
                val measured = textMeasurer.measure(glyph, style)
                drawText(
                    textMeasurer = textMeasurer,
                    text = glyph,
                    topLeft = Offset(
                        x = (width - measured.size.width) / 2f,
                        y = topPadding + pinPx * PIN_GLYPH_CENTER_RATIO - measured.size.height / 2f,
                    ),
                    style = style,
                )
            }

            if (comments != null) {
                drawBadge(comments, width, pinPx, topPadding)
            }
        }
    }

    private fun DrawScope.drawBadge(comments: Long, width: Int, pinPx: Float, topPadding: Float) {
        val cx = width / 2f + BADGE_OFFSET_X_DP.dp.toPx()
        val cy = topPadding + pinPx - BADGE_OFFSET_Y_DP.dp.toPx()

        drawCircle(
            color = palette.badgeBackground,
            radius = BADGE_RADIUS_DP.dp.toPx(),
            center = Offset(cx, cy),
        )

        val style = TextStyle(
            color = palette.badgeText,
            fontSize = BADGE_TEXT_DP.dp.toSp(),
            fontWeight = FontWeight.Bold,
        )
        val label = if (comments > MAX_COMMENT_BADGE) "9+" else comments.toString()
        val measured = textMeasurer.measure(label, style)
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f),
            style = style,
        )
    }

    private fun draw(width: Int, height: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(bitmap),
            size = Size(width.toFloat(), height.toFloat()),
            block = block,
        )
        return bitmap
    }

    /**
     * The glyph the icon font can actually draw, or null when no font is
     * available. Unknown names would render as tofu, so they fall back to the
     * same storefront glyph the Android implementation uses.
     */
    internal fun renderableGlyph(character: String?): String? {
        if (character.isNullOrEmpty() || iconFont == null) return null
        resolvedGlyphs[character]?.let { return it }

        val style = TextStyle(fontFamily = iconFont, fontSize = GLYPH_MEASURE_SIZE)
        val em = with(density) { GLYPH_MEASURE_SIZE.toPx() }
        val renderable = textMeasurer.measure(character, style).size.width < em * SINGLE_GLYPH_MAX_WIDTH_RATIO
        val glyph = if (renderable) character else FALLBACK_ICON
        resolvedGlyphs[character] = glyph
        return glyph
    }

    private fun scaledPin(pinPx: Float): Path {
        val scale = pinPx / PIN_VIEWPORT
        val matrix = Matrix().apply { scale(scale, scale, 1f) }
        return PathParser().parsePathString(PIN_PATH_DATA).toPath().apply { transform(matrix) }
    }

    private companion object {
        const val PIN_SIZE_DP = 48f
        const val GLYPH_TEXT_DP = 24f
        const val PIN_GLYPH_CENTER_RATIO = 0.40f
        const val BADGE_OFFSET_X_DP = 13f
        const val BADGE_OFFSET_Y_DP = 43f
        const val BADGE_RADIUS_DP = 9f
        const val BADGE_TEXT_DP = 11f
        const val BADGE_TOP_PADDING_DP = 10f
        const val FALLBACK_ICON = "storefront"
        const val SINGLE_GLYPH_MAX_WIDTH_RATIO = 1.5f
        const val PIN_VIEWPORT = 24f
        const val ICON_PADDING_PX = 4

        val GLYPH_MEASURE_SIZE = 100.sp

        /** The glyph each place icon name resolved to, so the measurement runs once. */
        val resolvedGlyphs = mutableMapOf<String, String>()

        /** The same outline as `R.drawable.map_marker` (viewport 24x24). */
        const val PIN_PATH_DATA =
            "m12,0.2589c4.9142,0 9.3603,3.7675 9.3603,9.5943C21.3603,13.7377 18.2363,18.336 12,23.6596 " +
                "5.7637,18.336 2.6397,13.7377 2.6397,9.8532 2.6397,4.0264 7.0858,0.2589 12,0.2589Z"
    }
}
