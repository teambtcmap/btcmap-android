package org.btcmap.map

import org.btcmap.db.table.area.AreaGeometry
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class AreaGeometryCacheTest {
    private val now = ZonedDateTime.parse("2026-06-01T00:00:00Z")

    @Test
    fun getOrPut_reusesTheGeometryWhileUpdatedAtIsUnchanged() {
        val cache = AreaGeometryCache(maxEntries = 10, maxPoints = 1_000)
        var parses = 0
        val parse = {
            parses++
            geometry(5)
        }

        cache.getOrPut(1L, now, parse)
        cache.getOrPut(1L, now, parse)

        Assert.assertEquals(1, parses)
        Assert.assertEquals(1, cache.size)
    }

    @Test
    fun getOrPut_reparsesWhenUpdatedAtChanges() {
        val cache = AreaGeometryCache(maxEntries = 10, maxPoints = 1_000)
        var parses = 0
        val parse = {
            parses++
            geometry(5)
        }

        cache.getOrPut(1L, now, parse)
        cache.getOrPut(1L, now.plusDays(1), parse)

        Assert.assertEquals(2, parses)
        Assert.assertEquals(1, cache.size)
    }

    @Test
    fun evictsTheLeastRecentlyUsedWhenTheEntryCapIsExceeded() {
        val cache = AreaGeometryCache(maxEntries = 2, maxPoints = 1_000_000)
        cache.getOrPut(1L, now) { geometry(4) }
        cache.getOrPut(2L, now) { geometry(4) }
        cache.getOrPut(3L, now) { geometry(4) }

        // 1 was the least recently used, so it was evicted; 2 and 3 remain.
        Assert.assertEquals(2, cache.size)
        var parses = 0
        cache.getOrPut(2L, now) {
            parses++
            geometry(4)
        }
        Assert.assertEquals(0, parses)
    }

    @Test
    fun evictsToThePointBudget() {
        val cache = AreaGeometryCache(maxEntries = 100, maxPoints = 10)
        val five = geometry(5)
        Assert.assertEquals(5, five.pointCount)

        cache.getOrPut(1L, now) { five }
        cache.getOrPut(2L, now) { five }
        // The third pushes the total to 15, past the budget of 10, so the least
        // recently used one is dropped.
        cache.getOrPut(3L, now) { five }

        Assert.assertEquals(2, cache.size)
    }

    @Test
    fun keepsTheNewestGeometryEvenWhenItAloneExceedsTheBudget() {
        val cache = AreaGeometryCache(maxEntries = 100, maxPoints = 5)
        val eight = geometry(8)

        cache.getOrPut(1L, now) { eight }
        Assert.assertEquals(1, cache.size)

        cache.getOrPut(2L, now) { eight }
        Assert.assertEquals(1, cache.size)

        // The newest entry survives its own over-budget insertion.
        var parses = 0
        cache.getOrPut(2L, now) {
            parses++
            geometry(8)
        }
        Assert.assertEquals(0, parses)
    }

    /** A polygon with exactly [points] coordinate pairs. */
    private fun geometry(points: Int): AreaGeometry {
        val ring = (0 until points).joinToString(separator = ",") { index -> "[$index,0]" }
        return AreaGeometry.parse("""{"type":"Polygon","coordinates":[[$ring]]}""")
    }
}
