package org.btcmap.map

import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

class EventsCache(
    map: MapLibreMap,
    private val db: Database,
) : ViewportCache<Event>(map) {
    override suspend fun fetch(bounds: LatLngBounds): Set<Event> {
        val (lonRange1, lonRange2) = bounds.splitAtAntimeridian()
        return if (lonRange2 == null) {
            db.event.selectByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange1.first,
                lonRange1.second,
            ).toHashSet()
        } else {
            val first = db.event.selectByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange1.first,
                lonRange1.second,
            )
            val second = db.event.selectByBounds(
                bounds.latitudeSouth,
                bounds.latitudeNorth,
                lonRange2.first,
                lonRange2.second,
            )
            (first + second).toHashSet()
        }
    }

    override fun Set<Event>.toGeoJson(): String = toEventGeoJson()

    override fun idOf(item: Event): Long = item.id
}
