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
    private val onMarkers: suspend (Set<Marker>) -> Unit,
) : ViewportCache<Marker>(map, source) {

    override suspend fun fetch(bounds: LatLngBounds): Set<Marker> {
        val (lonRange1, lonRange2) = bounds.splitAtAntimeridian()
        return if (lonRange2 == null) {
            db.place.selectMerchantsByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange1.first,
                lonRange1.second,
                minVerifiedAt = null,
            ).toHashSet()
        } else {
            val first = db.place.selectMerchantsByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange1.first,
                lonRange1.second,
                minVerifiedAt = null,
            )
            val second = db.place.selectMerchantsByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange2.first,
                lonRange2.second,
                minVerifiedAt = null,
            )
            (first + second).toHashSet()
        }
    }

    override suspend fun onSnapshot(snapshot: Set<Marker>) {
        // The marker images the GeoJSON references must exist in the style
        // before it is drawn.
        onMarkers(snapshot)
    }

    override fun Set<Marker>.toGeoJson(): String = toMarkerGeoJson()

    override fun idOf(item: Marker): Long = item.id
}
