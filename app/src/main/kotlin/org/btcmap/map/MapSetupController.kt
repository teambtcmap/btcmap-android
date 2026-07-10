package org.btcmap.map

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import org.btcmap.R
import org.btcmap.map.layer.createEventLayers
import org.btcmap.map.layer.createExchangeLayers
import org.btcmap.map.layer.createMerchantLayers
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

class MapSetupController(
    private val mapView: MapView,
    private val styleUri: String,
    private val markerBackgroundColor: Int,
    private val markerBadgeBackgroundColor: Int,
    private val markerBadgeTextColor: Int,
    private val boostedMarkerBackgroundColor: Int,
    private val usingOpenFreeMap: Boolean,
) {
    private val merchants = createMerchantLayers(
        markerBackgroundColor = markerBackgroundColor,
        markerBadgeBackgroundColor = markerBadgeBackgroundColor,
        markerBadgeTextColor = markerBadgeTextColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    private val events = createEventLayers(
        markerBackgroundColor = markerBackgroundColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    private val exchanges = createExchangeLayers(
        markerBackgroundColor = markerBackgroundColor,
        markerBadgeBackgroundColor = markerBadgeBackgroundColor,
        markerBadgeTextColor = markerBadgeTextColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    val merchantsSource: GeoJsonSource = merchants.first
    val eventsSource: GeoJsonSource = events.first
    val exchangesSource: GeoJsonSource = exchanges.first

    fun install() {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri(styleUri))
            map.uiSettings.setCompassMargins(0, dpToPx(120 + 16), dpToPx(16), 0)
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false

            map.getStyle { style ->
                if (style.getImage("btcmap-marker") == null) {
                    val drawable =
                        AppCompatResources.getDrawable(mapView.context, R.drawable.map_marker)!!
                    DrawableCompat.setTint(drawable, markerBackgroundColor)
                    style.addImage("btcmap-marker", drawable)
                }

                if (style.getImage("btcmap-marker-boosted") == null) {
                    val drawable =
                        AppCompatResources.getDrawable(mapView.context, R.drawable.map_marker)!!
                    DrawableCompat.setTint(drawable, boostedMarkerBackgroundColor)
                    style.addImage("btcmap-marker-boosted", drawable)
                }

                init(mapView.context, style)

                style.addSource(merchants.first)
                merchants.second.forEach { style.addLayer(it) }
                style.addSource(events.first)
                events.second.forEach { style.addLayer(it) }
                style.addSource(exchanges.first)
                exchanges.second.forEach { style.addLayer(it) }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * mapView.resources.displayMetrics.density).toInt()
    }
}
