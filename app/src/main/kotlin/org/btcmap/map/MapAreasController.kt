package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.area.AreaGeometry
import org.btcmap.db.table.area.geoJsonGeometry
import org.btcmap.db.table.event.isWithin
import org.btcmap.i18n.getLocalizedName
import org.btcmap.util.isUpcoming
import java.time.ZonedDateTime

/**
 * Finds the communities and countries containing the map centre in the local
 * cache, so panning the map costs no network round trip and works offline.
 *
 * Areas are synced in full by [org.btcmap.sync.Sync], and their cached GeoJSON is
 * tested with the same bbox pre-filter and point-in-polygon rules the server
 * uses, so the chips match what `GET /v4/areas?lat=&lon=` used to return.
 */
class MapAreasController(
    private val db: Database,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentJob: Job? = null
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var hasLastLocation = false

    // Parsed geometry is expensive to rebuild (a country's polygon can be
    // large) and only changes when the area is resynced, so it is kept by area
    // id and invalidated on updated_at. The access-ordered LRU is bounded by
    // both entry count and total points so a long session, or areas deleted
    // server-side, cannot grow it without limit.
    private val geometryCache = AreaGeometryCache(
        maxEntries = MAX_CACHED_GEOMETRIES,
        maxPoints = MAX_CACHED_GEOMETRY_POINTS,
    )

    private val _areas = MutableStateFlow<List<MapArea>>(emptyList())
    val areas: StateFlow<List<MapArea>> = _areas.asStateFlow()

    fun load(lat: Double, lon: Double) {
        lastLat = lat
        lastLon = lon
        hasLastLocation = true

        currentJob?.cancel("superseded by a newer map position")
        currentJob = scope.launch {
            val found = withContext(Dispatchers.IO) { lookup(lat, lon) }
            _areas.value = found
        }
    }

    /** Re-runs the last lookup, e.g. after areas were resynced. */
    fun reload() {
        if (hasLastLocation) load(lastLat, lastLon)
    }

    private fun clear() {
        currentJob?.cancel("map view destroyed")
        currentJob = null
        hasLastLocation = false
        _areas.value = emptyList()
    }

    fun dispose() {
        clear()
        scope.cancel()
    }

    private fun lookup(lat: Double, lon: Double): List<MapArea> {
        val contained = db.area.selectByBbox(
            west = lon - BBOX_EXPANSION,
            south = lat - BBOX_EXPANSION,
            east = lon + BBOX_EXPANSION,
            north = lat + BBOX_EXPANSION,
        )
            .filter { it.type == COMMUNITY_TYPE || it.type == COUNTRY_TYPE }
            .filter { it.urlAlias != EARTH_ALIAS }
            .mapNotNull { area ->
                val geometry = geometryFor(area)
                if (geometry.contains(lat, lon)) area to geometry else null
            }

        if (contained.isEmpty()) return emptyList()

        val counts = upcomingEventCounts(contained)
        // Countries lead the chips as the broader context; the sort is stable,
        // so communities keep the order they came back in.
        return contained.map { (area, _) ->
            MapArea(
                id = area.id,
                name = area.getLocalizedName(),
                type = area.type,
                urlAlias = area.urlAlias,
                upcomingEventsCount = counts[area.id] ?: 0,
                headerImageUrl = area.iconWide ?: area.icon,
            )
        }.sortedBy { if (it.type == COUNTRY_TYPE) 0 else 1 }
    }

    private fun geometryFor(area: Area): AreaGeometry {
        // Synchronized: a superseded lookup keeps running after cancel (the
        // nullary work does not suspend), so two lookups can touch the cache at
        // once. Holding the lock while parsing also avoids parsing twice.
        return synchronized(geometryCache) {
            geometryCache.getOrPut(area.id, area.updatedAt) { area.geoJsonGeometry() }
        }
    }

    /** The number of cached geometries; exposed so tests can assert the cap. */
    internal val cachedGeometryCount: Int
        get() = synchronized(geometryCache) { geometryCache.size }

    /**
     * Counts the upcoming events whose location falls inside each area,
     * mirroring the server: pre-filter by the union bounding box, then run the
     * precise point-in-polygon test per area.
     */
    private fun upcomingEventCounts(areas: List<Pair<Area, AreaGeometry>>): Map<Long, Int> {
        val west = areas.mapNotNull { it.first.bboxWest }.minOrNull() ?: return emptyMap()
        val south = areas.mapNotNull { it.first.bboxSouth }.minOrNull() ?: return emptyMap()
        val east = areas.mapNotNull { it.first.bboxEast }.maxOrNull() ?: return emptyMap()
        val north = areas.mapNotNull { it.first.bboxNorth }.maxOrNull() ?: return emptyMap()

        val now = now()
        val upcoming = db.event.selectByBounds(south, north, west, east)
            .filter { it.startsAt.isUpcoming(now) }

        return areas.associate { (area, geometry) ->
            area.id to upcoming.count { it.isWithin(geometry) }
        }
    }

    companion object {
        // The server expands the search box before its bbox pre-filter so that
        // an area whose bounding box does not contain the point but whose
        // polygon does is still considered.
        private const val BBOX_EXPANSION = 0.1

        private const val COMMUNITY_TYPE = "community"
        private const val COUNTRY_TYPE = "country"

        // The whole-world area would contain every point; the server skips it.
        private const val EARTH_ALIAS = "earth"

        internal const val MAX_CACHED_GEOMETRIES = 256

        // The entry cap still allows a lot of coordinate memory when every
        // cached area is a country-sized polygon, so the total is capped too.
        // The bundled snapshot's largest polygon is ~12k points.
        internal const val MAX_CACHED_GEOMETRY_POINTS = 200_000
    }
}
