package org.btcmap.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.btcmap.db.table.place.Marker
import org.btcmap.map.toMarkerGeoJson
import org.btcmap.ui.map.MerchantLayers
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * Phase 3 spike: the shared MapLibre Compose map. Renders the app's base style
 * with the ported merchant layer pipeline.
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
    usingOpenFreeMap: Boolean,
    modifier: Modifier = Modifier,
) {
    val geoJson = remember(markers) { markers.toMarkerGeoJson() }

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(initialLon, initialLat),
            zoom = initialZoom,
        ),
    ) {
        MerchantLayers(
            geoJson = geoJson,
            markerColor = markerBackgroundColor,
            markerIconColor = markerIconColor,
            boostedMarkerColor = boostedMarkerBackgroundColor,
            usingOpenFreeMap = usingOpenFreeMap,
        )
    }

    AppTheme {
        MaplibreMap(modifier = modifier.fillMaxSize(), state = state)
    }
}
