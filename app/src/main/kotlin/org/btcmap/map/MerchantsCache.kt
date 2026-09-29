package org.btcmap.map

import org.btcmap.db.Database
import org.btcmap.db.table.place.Marker
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource

class MerchantsCache(
    map: MapLibreMap,
    private val db: Database,
    source: GeoJsonSource,
    onFirstDataDrawn: (() -> Unit)? = null,
    private val onMarkers: suspend (Set<Marker>) -> Unit,
) : ViewportCache<Marker>(map, source, onFirstDataDrawn) {

    override suspend fun fetch(bounds: LatLngBounds): Set<Marker> {
        return bounds.queryByBounds { minLat, maxLat, minLon, maxLon ->
            db.place.selectMerchantsByBounds(
                minLat,
                maxLat,
                minLon,
                maxLon,
                minVerifiedAt = null,
            )
        }.toHashSet()
    }

    override suspend fun onSnapshot(snapshot: Set<Marker>) {
        // The marker images the GeoJSON references must exist in the style
        // before it is drawn.
        onMarkers(snapshot)
    }

    override fun Set<Marker>.toGeoJson(): String = toMarkerGeoJson()

    override fun idOf(item: Marker): Long = item.id
}
