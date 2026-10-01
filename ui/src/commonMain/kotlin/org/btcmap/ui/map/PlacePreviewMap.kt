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
import org.btcmap.db.table.place.Marker
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.markerImageName
import org.btcmap.map.toMarkerGeoJson
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * The place's own preview map, non-interactive, ported from the `MapView` at the
 * top of `place_fragment.xml`. It shows the place's marker with the same bitmap
 * the main map draws.
 */
@Composable
fun PlacePreviewMap(
    lat: Double,
    lon: Double,
    marker: Marker?,
    styleUrl: String,
    styleJson: String? = null,
    palette: MarkerPalette,
    usingOpenFreeMap: Boolean,
    iconFont: FontFamily?,
    modifier: Modifier = Modifier,
) {
    val geoJson = remember(marker) {
        marker?.let { listOf(it).toMarkerGeoJson() } ?: EMPTY_GEOJSON
    }
    val imageName = remember(marker) { marker?.markerImageName() }

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

    // The marker layer may only be declared once its image exists in the style.
    var imagesReady by remember { mutableStateOf(false) }

    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(lon, lat),
            zoom = PREVIEW_ZOOM,
        ),
    ) {
        MerchantLayers(
            geoJson = geoJson,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
            onMarkerClick = { ClickResult.Pass },
        )
    }

    val loadState = state.style.loadState
    LaunchedEffect(state, factory, imageName, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect
        if (imageName == null || marker == null) {
            imagesReady = true
            return@LaunchedEffect
        }
        imagesReady = false
        withFrameNanos { }
        state.style.images.set(imageName, factory.merchantMarker(marker))
        imagesReady = true
    }

    MaplibreMap(
        modifier = modifier,
        state = state,
        interactions = MapInteractions.None,
        uiOptions = MapUiOptions.None,
    )
}

private const val PREVIEW_ZOOM = 15.0
