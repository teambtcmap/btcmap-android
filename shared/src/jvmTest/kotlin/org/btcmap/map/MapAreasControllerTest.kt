package org.btcmap.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.btcmap.db.table.area.Area
import org.btcmap.db.testDatabase
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * The geometry cache [MapAreasController] keeps across map moves. Ported from
 * the Android instrumented suite: the controller is shared, so it belongs here.
 * It dispatches on `Dispatchers.Main`, so a main dispatcher is installed for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapAreasControllerTest {

    @Before
    fun installMainDispatcher() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun geometryCache_isBoundedWhenManyAreasAreTouched() = runBlocking<Unit> {
        val count = MapAreasController.MAX_CACHED_GEOMETRIES + 5
        val db = testDatabase()
        db.area.insert((1..count).map { area(it.toLong()) })

        val controller = MapAreasController(db = db)
        try {
            controller.load(LAT, LON)
            controller.awaitAreas()

            // Every area contains the point, so every polygon is parsed and
            // cached; the least recently used are then evicted down to the cap.
            Assert.assertEquals(count, controller.areas.value.size)
            Assert.assertEquals(
                MapAreasController.MAX_CACHED_GEOMETRIES,
                controller.cachedGeometryCount,
            )
        } finally {
            controller.dispose()
        }
    }

    @Test
    fun reload_reusesCachedGeometryForUnchangedAreas() = runBlocking<Unit> {
        val db = testDatabase()
        db.area.insert(listOf(area(1L)))

        val controller = MapAreasController(db = db)
        try {
            controller.load(LAT, LON)
            controller.awaitAreas()

            controller.reload()
            controller.awaitAreas()

            // The area's updated_at did not change, so its polygon is served
            // from the cache instead of being parsed again.
            Assert.assertEquals(1, controller.cachedGeometryCount)
        } finally {
            controller.dispose()
        }
    }

    /** Waits for the map-move lookup launched by `load`/`reload` to land. */
    private suspend fun MapAreasController.awaitAreas() = withTimeout(10_000) {
        while (areas.value.isEmpty()) delay(10)
    }

    private fun area(id: Long): Area {
        val west = LON - 0.05
        val east = LON + 0.05
        val south = LAT - 0.05
        val north = LAT + 0.05
        return Area(
            id = id,
            name = "Area $id",
            type = "community",
            urlAlias = "area-$id",
            icon = null,
            iconWide = null,
            websiteUrl = "https://btcmap.org/community/area-$id",
            description = null,
            bboxWest = west,
            bboxSouth = south,
            bboxEast = east,
            bboxNorth = north,
            geoJson = """{"type":"Polygon","coordinates":[[[$west,$south],[$east,$south],[$east,$north],[$west,$north],[$west,$south]]]}""",
        )
    }

    private companion object {
        const val LAT = 48.86
        const val LON = 2.35
    }
}
