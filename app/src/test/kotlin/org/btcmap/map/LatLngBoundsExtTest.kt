package org.btcmap.map

import org.junit.Assert
import org.junit.Test
import org.maplibre.android.geometry.LatLngBounds

class LatLngBoundsExtTest {

    @Test
    fun expand_doublesTheSpans() {
        val bounds = LatLngBounds.from(
            latNorth = 10.0,
            lonEast = 20.0,
            latSouth = 0.0,
            lonWest = 0.0,
        )

        val expanded = bounds.expand()

        Assert.assertEquals(15.0, expanded.latitudeNorth, EPSILON)
        Assert.assertEquals(-5.0, expanded.latitudeSouth, EPSILON)
        Assert.assertEquals(30.0, expanded.longitudeEast, EPSILON)
        Assert.assertEquals(-10.0, expanded.longitudeWest, EPSILON)
    }

    @Test
    fun expand_clampsLatitudeToThePoles() {
        val bounds = LatLngBounds.from(
            latNorth = 90.0,
            lonEast = 10.0,
            latSouth = 89.0,
            lonWest = 0.0,
        )

        val expanded = bounds.expand()

        Assert.assertEquals(90.0, expanded.latitudeNorth, EPSILON)
        Assert.assertTrue(expanded.latitudeSouth >= -90.0)
    }

    @Test
    fun expand_whenViewportCrossesAntimeridian_doesNotThrow() {
        // MapLibre reports a crossing viewport with east < west; the old
        // centre/span arithmetic built an invalid bounds and threw here.
        val bounds = wrappedBounds(latNorth = 10.0, lonEast = -170.0, latSouth = 0.0, lonWest = 170.0)

        val expanded = bounds.expand()

        Assert.assertEquals(200.0, expanded.longitudeEast, EPSILON)
        Assert.assertEquals(160.0, expanded.longitudeWest, EPSILON)
    }

    @Test
    fun expand_worldView_staysWithinTheWorld() {
        val bounds = LatLngBounds.from(
            latNorth = 90.0,
            lonEast = 180.0,
            latSouth = -90.0,
            lonWest = -180.0,
        )

        val expanded = bounds.expand()

        Assert.assertEquals(-180.0, expanded.longitudeWest, EPSILON)
        Assert.assertEquals(180.0, expanded.longitudeEast, EPSILON)
    }

    @Test
    fun splitAtAntimeridian_whenNotCrossing_returnsOneRange() {
        val bounds = LatLngBounds.from(
            latNorth = 10.0,
            lonEast = 20.0,
            latSouth = 0.0,
            lonWest = 0.0,
        )

        val (first, second) = bounds.splitAtAntimeridian()

        Assert.assertEquals(0.0 to 20.0, first)
        Assert.assertNull(second)
    }

    @Test
    fun splitAtAntimeridian_whenEastAbove180_wrapsBothHalves() {
        val bounds = LatLngBounds.from(
            latNorth = 10.0,
            lonEast = 185.0,
            latSouth = 0.0,
            lonWest = 175.0,
        )

        val (first, second) = bounds.splitAtAntimeridian()

        Assert.assertEquals(-180.0 to -175.0, first)
        Assert.assertEquals(175.0 to 180.0, second)
    }

    @Test
    fun splitAtAntimeridian_whenWestBelowMinus180_wrapsBothHalves() {
        // Reachable after expand(): the west edge can fall past -180 while the
        // east edge stays inside the usual range. The old code left the west
        // edge below -180 and queried a range the database can never hold.
        val bounds = LatLngBounds.from(
            latNorth = 10.0,
            lonEast = -165.5,
            latSouth = 0.0,
            lonWest = -183.5,
        )

        val (first, second) = bounds.splitAtAntimeridian()

        Assert.assertEquals(-180.0 to -165.5, first)
        Assert.assertEquals(176.5 to 180.0, second)
    }

    @Test
    fun splitAtAntimeridian_preservesTheWholeExpandedRange() {
        val bounds = wrappedBounds(latNorth = 10.0, lonEast = -170.0, latSouth = 0.0, lonWest = 170.0)

        val (first, second) = bounds.expand().splitAtAntimeridian()

        Assert.assertEquals(-180.0 to -160.0, first)
        Assert.assertEquals(160.0 to 180.0, second)
    }

    @Test
    fun queryByBounds_whenNotCrossing_runsOneQuery() {
        val bounds = LatLngBounds.from(
            latNorth = 10.0,
            lonEast = 20.0,
            latSouth = 0.0,
            lonWest = 0.0,
        )
        val ranges = mutableListOf<List<Double>>()

        val result = bounds.queryByBounds { minLat, maxLat, minLon, maxLon ->
            ranges.add(listOf(minLat, maxLat, minLon, maxLon))
            listOf("a")
        }

        Assert.assertEquals(listOf(listOf(0.0, 10.0, 0.0, 20.0)), ranges)
        Assert.assertEquals(listOf("a"), result)
    }

    @Test
    fun queryByBounds_whenCrossing_runsBothRangesAndMerges() {
        val bounds = wrappedBounds(latNorth = 10.0, lonEast = -170.0, latSouth = 0.0, lonWest = 170.0)
        val ranges = mutableListOf<Pair<Double, Double>>()

        val result = bounds.queryByBounds { _, _, minLon, maxLon ->
            ranges.add(minLon to maxLon)
            listOf(minLon)
        }

        Assert.assertEquals(listOf(-180.0 to -170.0, 170.0 to 180.0), ranges)
        Assert.assertEquals(listOf(-180.0, 170.0), result)
    }

    /**
     * MapLibre's public factories reject east < west, but the renderer can
     * report a crossing viewport through the internal constructor it uses
     * itself, so the test builds one the same way.
     */
    private fun wrappedBounds(
        latNorth: Double,
        lonEast: Double,
        latSouth: Double,
        lonWest: Double,
    ): LatLngBounds {
        val constructor = LatLngBounds::class.java.getDeclaredConstructor(
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
        )
        constructor.isAccessible = true
        return constructor.newInstance(latNorth, lonEast, latSouth, lonWest)
    }

    private companion object {
        const val EPSILON = 1e-6
    }
}
