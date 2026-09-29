package org.btcmap.map

import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import org.btcmap.R
import org.btcmap.db.table.place.Marker
import org.btcmap.map.layer.createEventLayers
import org.btcmap.map.layer.createExchangeLayers
import org.btcmap.map.layer.createMerchantLayers
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

internal class MapSetupController(
    private val mapView: MapView,
    private val styleUri: String,
    /**
     * The bundled-basemap rewrite of the style (see [rewriteForBundledBasemap]),
     * or null to load [styleUri] as-is. It also carries the layer ids flipped
     * when connectivity changes.
     */
    private val bundledStyle: BundledBasemapStyle? = null,
    private val markerBackgroundColor: Int,
    private val markerIconColor: Int,
    private val markerBadgeBackgroundColor: Int,
    private val markerBadgeTextColor: Int,
    private val boostedMarkerBackgroundColor: Int,
    private val boostedMarkerIconColor: Int,
    private val usingOpenFreeMap: Boolean,
    private val rotationEnabled: Boolean,
) {
    private var style: Style? = null

    /**
     * Whether the bundled low-zoom tiles should overzoom all the way up. Set by
     * [setOffline] from the connectivity callbacks, and applied again whenever
     * the style (re)loads so the first frame already matches the network.
     */
    private var offline = false

    /**
     * Kept per controller (and so per map view), not process-wide, so a new map
     * resetting its own marker images cannot invalidate another map's masks.
     */
    val markerImageRegistry = MarkerImageRegistry()

    private val merchants = createMerchantLayers(
        markerBackgroundColor = markerBackgroundColor,
        markerIconColor = markerIconColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    private val events = createEventLayers(
        markerBackgroundColor = markerBackgroundColor,
        markerIconColor = markerIconColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    private val exchanges = createExchangeLayers(
        markerBackgroundColor = markerBackgroundColor,
        markerIconColor = markerIconColor,
        markerBadgeBackgroundColor = markerBadgeBackgroundColor,
        markerBadgeTextColor = markerBadgeTextColor,
        usingOpenFreeMap = usingOpenFreeMap,
    )

    val merchantsSource: GeoJsonSource = merchants.first
    val eventsSource: GeoJsonSource = events.first
    val exchangesSource: GeoJsonSource = exchanges.first

    fun install() {
        markerImageRegistry.clear()
        mapView.getMapAsync { map ->
            map.setStyle(
                if (bundledStyle != null) {
                    Style.Builder().fromJson(bundledStyle.json)
                } else {
                    Style.Builder().fromUri(styleUri)
                },
            )
            map.uiSettings.setCompassMargins(0, dpToPx(120 + 16), dpToPx(16), 0)
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            map.uiSettings.isRotateGesturesEnabled = rotationEnabled

            map.getStyle { style ->
                this.style = style

                if (style.getImage("btcmap-marker") == null) {
                    val drawable =
                        AppCompatResources.getDrawable(mapView.context, R.drawable.map_marker)!!
                            .mutate()
                    DrawableCompat.setTint(drawable, markerBackgroundColor)
                    style.addImage("btcmap-marker", drawable)
                }

                if (style.getImage("btcmap-marker-boosted") == null) {
                    val drawable =
                        AppCompatResources.getDrawable(mapView.context, R.drawable.map_marker)!!
                            .mutate()
                    DrawableCompat.setTint(drawable, boostedMarkerBackgroundColor)
                    style.addImage("btcmap-marker-boosted", drawable)
                }

                ensureEventMarkerImage(mapView.context, style, markerIconColor)

                // The plain pin exchange and event markers draw, so their taps
                // are hit-tested through its transparent pixels too.
                markerImageRegistry.addPinMask(markerPinAlphaMask(mapView.context))

                style.addSource(merchants.first)
                merchants.second.forEach { style.addLayer(it) }
                style.addSource(events.first)
                events.second.forEach { style.addLayer(it) }
                style.addSource(exchanges.first)
                exchanges.second.forEach { style.addLayer(it) }

                applyOffline()
            }
        }
    }

    /**
     * Switches the bundled basemap between the split (online) and the
     * all-overzoomed (offline) behaviour. Safe to call before the style has
     * loaded: the state is applied when it does.
     */
    fun setOffline(offline: Boolean) {
        this.offline = offline
        applyOffline()
    }

    private fun applyOffline() {
        val style = style ?: return
        val bundled = bundledStyle ?: return

        // Offline shows the archive under the hosted layers, so a tile MapLibre
        // has cached still draws in detail and only the rest falls back to the
        // overzoomed z4. Online the fallback is hidden and the hosted tiles are
        // used at every zoom.
        val visibility = if (offline) Property.VISIBLE else Property.NONE
        for (id in bundled.fallbackLayers) {
            style.getLayer(id)?.setProperties(PropertyFactory.visibility(visibility))
        }
    }

    suspend fun ensureMerchantMarkers(markers: Set<Marker>) {
        val style = style ?: return
        ensureMerchantMarkerImages(
            context = mapView.context,
            style = style,
            markers = markers,
            markerBackgroundColor = markerBackgroundColor,
            markerIconColor = markerIconColor,
            boostedMarkerBackgroundColor = boostedMarkerBackgroundColor,
            boostedMarkerIconColor = boostedMarkerIconColor,
            markerBadgeBackgroundColor = markerBadgeBackgroundColor,
            markerBadgeTextColor = markerBadgeTextColor,
            registry = markerImageRegistry,
        )
    }

    suspend fun ensureExchangeMarkers(markers: Set<Marker>) {
        val style = style ?: return
        ensureExchangeMarkerImages(
            context = mapView.context,
            style = style,
            markers = markers,
            markerIconColor = markerIconColor,
            registry = markerImageRegistry,
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * mapView.resources.displayMetrics.density).toInt()
    }
}
