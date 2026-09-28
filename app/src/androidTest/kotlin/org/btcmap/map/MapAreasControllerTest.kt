package org.btcmap.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.db.table.area.Area
import org.btcmap.util.AppTestCase
import org.btcmap.util.waitUntil
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapAreasControllerTest : AppTestCase() {

    @Test
    fun geometryCache_isBoundedWhenManyAreasAreTouched() {
        val count = MapAreasController.MAX_CACHED_GEOMETRIES + 5
        databaseRule.db.area.insert((1..count).map { area(it.toLong()) })

        val controller = MapAreasController(db = databaseRule.db)
        try {
            controller.load(LAT, LON)
            waitUntil { controller.areas.value.isNotEmpty() }

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
    fun reload_reusesCachedGeometryForUnchangedAreas() {
        databaseRule.db.area.insert(listOf(area(1L)))

        val controller = MapAreasController(db = databaseRule.db)
        try {
            controller.load(LAT, LON)
            waitUntil { controller.areas.value.isNotEmpty() }

            controller.reload()
            waitUntil { controller.areas.value.isNotEmpty() }

            // The area's updated_at did not change, so its polygon is served
            // from the cache instead of being parsed again.
            Assert.assertEquals(1, controller.cachedGeometryCount)
        } finally {
            controller.dispose()
        }
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
