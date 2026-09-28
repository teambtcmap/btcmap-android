package org.btcmap.map

import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.util.isUpcoming
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import java.time.ZonedDateTime

class EventsCache(
    map: MapLibreMap,
    private val db: Database,
    source: GeoJsonSource,
) : ViewportCache<Event>(map, source) {
    override suspend fun fetch(bounds: LatLngBounds): Set<Event> {
        val now = ZonedDateTime.now()
        return bounds.queryByBounds { minLat, maxLat, minLon, maxLon ->
            db.event.selectByBounds(minLat, maxLat, minLon, maxLon)
        }.filter { it.startsAt.isUpcoming(now) }.toHashSet()
    }

    override fun Set<Event>.toGeoJson(): String = toEventGeoJson()

    override fun idOf(item: Event): Long = item.id
}
