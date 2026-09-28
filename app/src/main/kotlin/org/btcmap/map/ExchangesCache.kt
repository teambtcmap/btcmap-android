package org.btcmap.map

import org.btcmap.db.Database
import org.btcmap.db.table.place.Marker
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource

class ExchangesCache(
    map: MapLibreMap,
    private val db: Database,
    source: GeoJsonSource,
    private val onMarkers: suspend (Set<Marker>) -> Unit,
) : ViewportCache<Marker>(map, source) {

    override suspend fun fetch(bounds: LatLngBounds): Set<Marker> {
        return bounds.queryByBounds { minLat, maxLat, minLon, maxLon ->
            db.place.selectExchangesByBounds(minLat, maxLat, minLon, maxLon)
        }.toHashSet()
    }

    override suspend fun onSnapshot(snapshot: Set<Marker>) {
        onMarkers(snapshot)
    }

    override fun Set<Marker>.toGeoJson(): String = toMarkerGeoJson()

    override fun idOf(item: Marker): Long = item.id
}
