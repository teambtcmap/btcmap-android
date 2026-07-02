package org.btcmap.map

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.btcmap.db.Database
import org.btcmap.db.table.place.Marker
import org.btcmap.db.table.place.MarkerProjection
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

class MerchantsCache(
    private val map: MapLibreMap,
    private val db: Database,
) : MapLibreMap.OnCameraIdleListener {
    private val merchants: MutableSet<Marker> =
        mutableSetOf<MarkerProjection>().toHashSet()
    val geoJson = MutableStateFlow(merchants.toGeoJson())

    init {
        Log.d("merchants_cache", "init")
        map.addOnCameraIdleListener(this)
    }

    override fun onCameraIdle() {
        Log.d("merchants_cache", "camera idle")
        val bounds = map.projection.visibleRegion.latLngBounds
        Log.d("merchants_cache", "real map bounds: $bounds")
        val expandedBounds = expandBounds(bounds)
        val merchantsInBounds = db.place.selectMerchantsByBounds(
            expandedBounds.latitudeSouth,
            expandedBounds.latitudeNorth,
            expandedBounds.longitudeWest,
            expandedBounds.longitudeEast,
            minVerifiedAt = null,
        ).toHashSet()
        Log.d("merchants_cache", "merchants in bounds: ${merchantsInBounds.size}")
        merchants.addAll(merchantsInBounds)
        geoJson.update { merchants.toGeoJson() }
    }

    fun destroy() {
        Log.d("merchants_cache", "destroy")
        map.removeOnCameraIdleListener(this)
    }

    private fun expandBounds(bounds: LatLngBounds, scaleFactor: Double = 2.0): LatLngBounds {
        val latSpan = bounds.latitudeSpan * scaleFactor
        val lonSpan = bounds.longitudeSpan * scaleFactor
        val center = bounds.center
        val latNorth = (center.latitude + latSpan / 2).coerceAtMost(90.0)
        val latSouth = (center.latitude - latSpan / 2).coerceAtLeast(-90.0)
        return LatLngBounds.from(
            latNorth = latNorth,
            lonEast = center.longitude + lonSpan / 2,
            latSouth = latSouth,
            lonWest = center.longitude - lonSpan / 2,
        )
    }

    private fun Set<Marker>.toGeoJson(): String {
        val sb = StringBuilder()
        sb.append(
            """
        {
            "type": "FeatureCollection",
            "features": [
        """.trimIndent()
        )

        this.forEachIndexed { index, place ->
            if (index > 0) {
                sb.append(",")
            }
            sb.append(
                """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${place.lon}, ${place.lat}]
                },
                "properties": {
                    "id": ${place.id},
                    "count": 1,
                    "iconId": "${place.icon}",
                    "requiresCompanionApp": ${place.requiredAppUrl != null},
                    "comments": ${place.comments},
                    "boosted": ${place.boostedUntil != null}
                }
            }
        """.trimIndent()
            )
        }

        sb.append(
            """
            ]
        }
        """.trimIndent()
        )

        return sb.toString()
    }
}