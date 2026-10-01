package org.btcmap.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import org.btcmap.db.table.place.Marker
import org.btcmap.map.markerImageName
import org.btcmap.map.toMarkerGeoJson
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MerchantLayers
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import java.time.ZonedDateTime

/**
 * Phase 3 spike: the shared MapLibre Compose map. Renders the app's base style
 * with the ported merchant layer pipeline and marker bitmaps.
 */
@Composable
fun MapScreen(
    styleUrl: String,
    markers: List<Marker>,
    initialLat: Double,
    initialLon: Double,
    initialZoom: Double,
    markerBackgroundColor: Color,
    markerIconColor: Color,
    boostedMarkerBackgroundColor: Color,
    boostedMarkerIconColor: Color,
    markerBadgeBackgroundColor: Color,
    markerBadgeTextColor: Color,
    usingOpenFreeMap: Boolean,
    iconFont: FontFamily?,
    modifier: Modifier = Modifier,
) {
    val geoJson = remember(markers) { markers.toMarkerGeoJson() }
    val markersByName = remember(markers) {
        val now = ZonedDateTime.now()
        markers.associateBy { it.markerImageName(now) }
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val factory = remember(
        textMeasurer,
        iconFont,
        density,
        markerBackgroundColor,
        markerIconColor,
        boostedMarkerBackgroundColor,
        boostedMarkerIconColor,
        markerBadgeBackgroundColor,
        markerBadgeTextColor,
    ) {
        MarkerBitmapFactory(
            textMeasurer = textMeasurer,
            iconFont = iconFont,
            density = density,
            markerBackgroundColor = markerBackgroundColor,
            markerIconColor = markerIconColor,
            boostedMarkerBackgroundColor = boostedMarkerBackgroundColor,
            boostedMarkerIconColor = boostedMarkerIconColor,
            markerBadgeBackgroundColor = markerBadgeBackgroundColor,
            markerBadgeTextColor = markerBadgeTextColor,
        )
    }

    // The marker layer may only be declared once its images exist in the style:
    // a layer declared earlier keeps the image it resolved as missing.
    var imagesReady by remember { mutableStateOf(false) }

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(initialLon, initialLat),
            zoom = initialZoom,
        ),
    ) {
        MerchantLayers(
            geoJson = geoJson,
            clusterBackgroundColor = markerBackgroundColor,
            clusterTextColor = markerIconColor,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
        )
    }

    val loadState = state.style.loadState
    LaunchedEffect(state, factory, markersByName, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // Take the marker layer out for a frame so it is declared again, and so
        // resolves any names the new data added.
        imagesReady = false
        withFrameNanos { }

        val images = state.style.images
        markersByName.forEach { (name, marker) ->
            images.set(name, factory.merchantMarker(marker))
        }

        imagesReady = true
    }

    AppTheme(iconFont = iconFont) {
        MaplibreMap(modifier = modifier.fillMaxSize(), state = state)
    }
}
