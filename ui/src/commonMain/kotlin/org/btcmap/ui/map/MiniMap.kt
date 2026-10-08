package org.btcmap.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.btcmap.ui.LocalIconFont
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** How close a mini map opens: the pin and its immediate surroundings. */
private const val MINI_MAP_ZOOM = 14.0

/** The size a mini map pin is drawn at, down from the 48dp marker. */
private val MINI_MAP_PIN_SIZE = 32.dp

/**
 * A quiet, non-interactive map thumbnail centred on a coordinate, with a stock
 * pin over it. It drops the MapLibre UI (logo, attribution and controls), like
 * [PlacePreviewMap], so it reads as a static preview rather than a map to pan;
 * the surrounding screen carries the app's attribution.
 *
 * [pin] builds the bitmap to draw, so [EventMiniMap] and [NoteMiniMap] share one
 * implementation and differ only in the glyph they preview.
 */
@Composable
internal fun MiniMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    pin: (MarkerBitmapFactory) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(target = Position(lon, lat), zoom = MINI_MAP_ZOOM),
    ) { }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val iconFont = LocalIconFont.current
    val bitmap = remember(textMeasurer, iconFont, density, palette) {
        pin(
            MarkerBitmapFactory(
                textMeasurer = textMeasurer,
                iconFont = iconFont,
                density = density,
                palette = palette,
            )
        )
    }

    Box(modifier = modifier) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = state,
            interactions = MapInteractions.None,
            uiOptions = MapUiOptions.None,
            // The banner is a decorative preview, not a map to explore: drop the
            // MapLibre logo and attribution pill the same way the positioning
            // maps do. `overlay` is what removes them, not `MapUiOptions`.
            overlay = {},
        )
        // The pin's tip, not its centre, marks the coordinate: shift it up by
        // half its drawn height so it sits on the map centre.
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(MINI_MAP_PIN_SIZE)
                .offset(y = -(MINI_MAP_PIN_SIZE / 2)),
        )
    }
}
