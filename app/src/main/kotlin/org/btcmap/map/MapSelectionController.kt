package org.btcmap.map

import android.graphics.PointF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.map.layer.EVENT_MARKER_LAYER_ID
import org.btcmap.map.layer.EXCHANGE_MARKER_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

class MapSelectionController(
    private val map: MapLibreMap,
    private val db: Database,
    private val onOpenPlace: suspend (Place) -> Unit,
    private val onOpenEvent: (Event) -> Unit,
    private val onNoHit: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val clickListener = MapLibreMap.OnMapClickListener { handleClick(it) }

    fun install() {
        map.addOnMapClickListener(clickListener)
    }

    fun detach() {
        map.removeOnMapClickListener(clickListener)
        scope.cancel()
    }

    private fun handleClick(point: LatLng): Boolean {
        // queryRenderedFeatures crashes in native code while the style is still
        // loading, so ignore taps that arrive before it is ready.
        val style = map.style
        if (style == null || !style.isFullyLoaded) {
            onNoHit()
            return false
        }

        val screenLocation = map.projection.toScreenLocation(point)
        val features = map.queryRenderedFeatures(
            screenLocation,
            MERCHANT_MARKER_LAYER_ID,
            EXCHANGE_MARKER_LAYER_ID,
            EVENT_MARKER_LAYER_ID,
        )

        val feature = features.firstOrNull { isOpaqueHit(it, screenLocation) }

        if (feature == null) {
            onNoHit()
            return false
        }

        return try {
            val isEvent =
                feature.getProperty("iconId") == null && feature.getProperty("count") == null

            if (isEvent) {
                val idValue = feature.getProperty("id") ?: return false
                val eventId = idValue.asLong
                scope.launch {
                    try {
                        val event = withContext(Dispatchers.IO) {
                            db.event.selectById(eventId)
                        } ?: return@launch
                        onOpenEvent(event)
                    } catch (e: Throwable) {
                        e.rethrowIfCancellation()
                    }
                }
                return true
            }

            val idValue = feature.getProperty("id") ?: return false
            val placeId = idValue.asLong
            scope.launch {
                try {
                    val place = withContext(Dispatchers.IO) {
                        db.place.selectById(placeId)
                    } ?: return@launch
                    onOpenPlace(place)
                } catch (e: Throwable) {
                    e.rethrowIfCancellation()
                }
            }
            return true
        } catch (e: Exception) {
            false
        }
    }

    private fun isOpaqueHit(feature: Feature, screenLocation: PointF): Boolean {
        val iconId = feature.getStringProperty("iconId") ?: return true
        val name = merchantMarkerImageName(
            iconId = iconId,
            boosted = feature.getBooleanProperty("boosted") == true,
            outdated = feature.getBooleanProperty("outdated") == true,
            comments = feature.getNumberProperty("comments")?.toLong() ?: 0,
        )
        val mask = merchantMarkerMask(name) ?: return true
        val geometry = feature.geometry() as? Point ?: return true
        val anchor = map.projection.toScreenLocation(
            LatLng(geometry.latitude(), geometry.longitude())
        )
        val x = (screenLocation.x - (anchor.x - mask.width / 2f)).toInt()
        val y = (screenLocation.y - (anchor.y - mask.height)).toInt()
        return mask.isOpaque(x, y)
    }
}
