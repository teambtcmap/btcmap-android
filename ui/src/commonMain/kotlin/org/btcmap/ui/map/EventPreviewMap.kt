package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import org.btcmap.map.EVENT_ICON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * The event's own map, interactive, ported from the `MapView` in
 * `event_fragment.xml`. It draws the event marker with the same pin and glyph
 * the main map uses, and drops MapLibre's logo and attribution pill the way the
 * mini and positioning maps do.
 */
@Composable
fun EventPreviewMap(
    lat: Double,
    lon: Double,
    geoJson: String,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    usingOpenFreeMap: Boolean,
    iconFont: FontFamily?,
    onState: (MapState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val factory = remember(textMeasurer, iconFont, density, palette) {
        MarkerBitmapFactory(
            textMeasurer = textMeasurer,
            iconFont = iconFont,
            density = density,
            palette = palette,
        )
    }

    // The marker layers may only be declared once their images exist.
    var imagesReady by remember { mutableStateOf(false) }

    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(lon, lat),
            zoom = EVENT_ZOOM,
        ),
    ) {
        EventLayers(
            geoJson = geoJson,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
            onMarkerClick = { ClickResult.Pass },
        )
    }

    // Handed to the host so its zoom buttons can drive the camera.
    LaunchedEffect(state) {
        onState(state)
    }

    val loadState = state.style.loadState
    LaunchedEffect(state, factory, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect
        imagesReady = false
        withFrameNanos { }
        state.style.images.setBitmap(MARKER_PIN_IMAGE_ID, factory.pin(palette.markerBackground))
        state.style.images.setBitmap(EVENT_MARKER_ICON_NAME, factory.icon(EVENT_ICON, palette.markerIcon))
        imagesReady = true
    }

    MaplibreMap(
        modifier = modifier,
        state = state,
        // The event map is a detail preview, so drop MapLibre's logo and
        // expanding attribution pill the way the mini and positioning maps do.
        // `overlay` is what removes them, not `MapUiOptions`.
        overlay = {},
    )
}

private const val EVENT_ZOOM = 14.0
