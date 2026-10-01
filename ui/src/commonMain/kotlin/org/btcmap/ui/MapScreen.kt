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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Marker
import org.btcmap.map.EVENT_ICON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.exchangeMarkerIconImageName
import org.btcmap.map.markerImageName
import org.btcmap.map.toEventGeoJson
import org.btcmap.map.toMarkerGeoJson
import org.btcmap.ui.map.EventLayers
import org.btcmap.ui.map.ExchangeLayers
import org.btcmap.ui.map.MARKER_PIN_IMAGE_ID
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MarkerPalette
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
 * with the ported merchant, event and exchange layer pipelines.
 */
@Composable
fun MapScreen(
    styleUrl: String,
    markers: List<Marker>,
    exchanges: List<Marker>,
    events: List<Event>,
    initialLat: Double,
    initialLon: Double,
    initialZoom: Double,
    palette: MarkerPalette,
    usingOpenFreeMap: Boolean,
    iconFont: FontFamily?,
    modifier: Modifier = Modifier,
) {
    val merchantsGeoJson = remember(markers) { markers.toMarkerGeoJson() }
    val exchangesGeoJson = remember(exchanges) { exchanges.toMarkerGeoJson() }
    val eventsGeoJson = remember(events) { events.toEventGeoJson() }

    val markersByName = remember(markers) {
        val now = ZonedDateTime.now()
        markers.associateBy { it.markerImageName(now) }
    }
    val exchangeIcons = remember(exchanges) { exchanges.map { it.icon }.distinct() }

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

    // The marker layers may only be declared once their images exist in the
    // style: a layer declared earlier keeps the image it resolved as missing.
    var imagesReady by remember { mutableStateOf(false) }

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(initialLon, initialLat),
            zoom = initialZoom,
        ),
    ) {
        MerchantLayers(
            geoJson = merchantsGeoJson,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
        )
        EventLayers(
            geoJson = eventsGeoJson,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
        )
        ExchangeLayers(
            geoJson = exchangesGeoJson,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            badgeBackgroundColor = palette.badgeBackground,
            badgeTextColor = palette.badgeText,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
        )
    }

    val loadState = state.style.loadState
    LaunchedEffect(state, factory, markersByName, exchangeIcons, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // Take the marker layers out for a frame so they are declared again, and
        // so resolve any image names the new data added.
        imagesReady = false
        withFrameNanos { }

        val images = state.style.images
        images.set(MARKER_PIN_IMAGE_ID, factory.pin(palette.markerBackground))
        images.set(EVENT_MARKER_ICON_NAME, factory.icon(EVENT_ICON, palette.markerIcon))
        markersByName.forEach { (name, marker) ->
            images.set(name, factory.merchantMarker(marker))
        }
        exchangeIcons.forEach { icon ->
            images.set(exchangeMarkerIconImageName(icon), factory.icon(icon, palette.markerIcon))
        }

        imagesReady = true
    }

    AppTheme(iconFont = iconFont) {
        MaplibreMap(modifier = modifier.fillMaxSize(), state = state)
    }
}
