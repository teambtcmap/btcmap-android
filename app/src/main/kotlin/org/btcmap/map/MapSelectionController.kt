package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.btcmap.map.layer.EVENT_MARKER_LAYER_ID
import org.btcmap.map.layer.EXCHANGE_MARKER_LAYER_ID
import org.btcmap.map.layer.MERCHANT_MARKER_LAYER_ID
import org.maplibre.android.maps.MapLibreMap

class MapSelectionController(
    private val map: MapLibreMap,
    private val db: Database,
    private val onOpenPlace: suspend (Place) -> Unit,
    private val onOpenEventWebsite: (HttpUrl) -> Unit,
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

    private fun handleClick(point: org.maplibre.android.geometry.LatLng): Boolean {
        val screenLocation = map.projection.toScreenLocation(point)
        val features = map.queryRenderedFeatures(
            screenLocation,
            MERCHANT_MARKER_LAYER_ID,
            EXCHANGE_MARKER_LAYER_ID,
            EVENT_MARKER_LAYER_ID,
        )

        if (features.isEmpty()) {
            onNoHit()
            return false
        }

        val feature = features[0]

        try {
            val isEvent =
                feature.getProperty("iconId") == null && feature.getProperty("count") == null

            if (isEvent) {
                val idValue = feature.getProperty("id") ?: return false
                val eventId = idValue.asLong
                scope.launch {
                    val event = withContext(Dispatchers.IO) {
                        db.event.selectById(eventId)
                    } ?: return@launch
                    onOpenEventWebsite(event.website)
                }
                return true
            }

            val idValue = feature.getProperty("id") ?: return false
            val placeId = idValue.asLong
            scope.launch {
                val place = withContext(Dispatchers.IO) {
                    db.place.selectById(placeId)
                } ?: return@launch
                onOpenPlace(place)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
