package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.place.Marker
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import java.util.concurrent.atomic.AtomicReference

class MerchantsCache(
    private val map: MapLibreMap,
    private val db: Database,
) : MapLibreMap.OnCameraIdleListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingQuery = AtomicReference<Job?>(null)
    private val seenIds: MutableSet<Long> = mutableSetOf()
    private val merchants: MutableSet<Marker> = mutableSetOf()
    val geoJson = MutableStateFlow(merchants.toGeoJson())

    init {
        map.addOnCameraIdleListener(this)
    }

    override fun onCameraIdle() {
        val bounds = map.projection.visibleRegion.latLngBounds
        val expandedBounds = expandBounds(bounds)

        pendingQuery.getAndSet(
            scope.launch {
                val (lonRange1, lonRange2) = expandedBounds.toLonRanges()
                val merchantsInBounds = withContext(Dispatchers.IO) {
                    if (lonRange2 == null) {
                        db.place.selectMerchantsByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange1.first,
                            lonRange1.second,
                            minVerifiedAt = null,
                        ).toHashSet()
                    } else {
                        val first = db.place.selectMerchantsByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange1.first,
                            lonRange1.second,
                            minVerifiedAt = null,
                        )
                        val second = db.place.selectMerchantsByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange2.first,
                            lonRange2.second,
                            minVerifiedAt = null,
                        )
                        (first + second).toHashSet()
                    }
                }

                val newOnes = merchantsInBounds.filter { it.id !in seenIds }
                if (newOnes.isEmpty()) return@launch

                seenIds.addAll(newOnes.map { it.id })
                merchants.addAll(newOnes)

                val next = withContext(Dispatchers.Default) { merchants.toGeoJson() }
                geoJson.value = next
            }
        )?.cancel()
    }

    fun destroy() {
        map.removeOnCameraIdleListener(this)
        pendingQuery.getAndSet(null)?.cancel()
        scope.cancel()
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

    private fun LatLngBounds.toLonRanges(): Pair<Pair<Double, Double>, Pair<Double, Double>?> {
        var west = longitudeWest
        var east = longitudeEast
        if (west > 180.0) {
            west -= 360.0
        }
        if (east > 180.0) {
            east -= 360.0
        }
        return if (west <= east) {
            Pair(west to east, null)
        } else {
            Pair(-180.0 to east, west to 180.0)
        }
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