package org.btcmap.ui.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The viewport arithmetic the map queries its features with, moved here from the
 * Views `LatLngBounds` extensions the map used before the shared map took over.
 */
class ViewportBoundsTest {

    @Test
    fun expand_doublesTheSpans() {
        val expanded = ViewportBounds.expand(bounds(south = 0.0, north = 10.0, west = 0.0, east = 20.0))

        assertEquals(15.0, expanded.north, EPSILON)
        assertEquals(-5.0, expanded.south, EPSILON)
        assertEquals(30.0, expanded.east, EPSILON)
        assertEquals(-10.0, expanded.west, EPSILON)
    }

    @Test
    fun expand_clampsLatitudeToThePoles() {
        val expanded = ViewportBounds.expand(bounds(south = 89.0, north = 90.0, west = 0.0, east = 10.0))

        assertEquals(90.0, expanded.north, EPSILON)
        assertTrue(expanded.south >= -90.0)
    }

    @Test
    fun expand_worldView_staysWithinTheWorld() {
        val expanded = ViewportBounds.expand(
            bounds(south = -90.0, north = 90.0, west = -180.0, east = 180.0),
        )

        assertEquals(-180.0, expanded.west, EPSILON)
        assertEquals(180.0, expanded.east, EPSILON)
    }

    @Test
    fun longitudeRanges_whenNotCrossing_returnsOneRange() {
        val ranges = ViewportBounds.expand(
            bounds(south = 0.0, north = 10.0, west = 0.0, east = 20.0),
        ).longitudeRanges()

        assertEquals(listOf(-10.0 to 30.0), ranges)
    }

    @Test
    fun longitudeRanges_whenEastAbove180_wrapsBothHalves() {
        val ranges = ViewportBounds(south = 0.0, north = 10.0, west = 175.0, east = 185.0)
            .longitudeRanges()

        assertEquals(listOf(-180.0 to -175.0, 175.0 to 180.0), ranges)
    }

    @Test
    fun longitudeRanges_whenWestBelowMinus180_wrapsBothHalves() {
        // Reachable after expand(): the west edge can fall past -180 while the
        // east edge stays inside the usual range. The old code left the west
        // edge below -180 and queried a range the database can never hold.
        val ranges = ViewportBounds(south = 0.0, north = 10.0, west = -183.5, east = -165.5)
            .longitudeRanges()

        assertEquals(listOf(-180.0 to -165.5, 176.5 to 180.0), ranges)
    }

    @Test
    fun longitudeRanges_coversAWrappedExpandedBounds() {
        // What expand() produces where a viewport crosses the antimeridian: a
        // west edge below 180 and an east edge above it. VisibleBounds itself
        // refuses east < west, so the case is built here directly.
        val ranges = ViewportBounds(south = 0.0, north = 10.0, west = 160.0, east = 200.0)
            .longitudeRanges()

        assertEquals(listOf(-180.0 to -160.0, 160.0 to 180.0), ranges)
    }

    private fun bounds(
        south: Double,
        north: Double,
        west: Double,
        east: Double,
    ): ViewportBounds = ViewportBounds(south = south, north = north, west = west, east = east)

    private companion object {
        const val EPSILON = 1e-6
    }
}
