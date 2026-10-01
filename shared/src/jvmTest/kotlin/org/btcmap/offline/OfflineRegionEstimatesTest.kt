package org.btcmap.offline

import org.junit.Assert
import org.junit.Test

class OfflineRegionEstimatesTest {

    private val paris = OfflineBounds(west = 2.0, south = 48.0, east = 3.0, north = 49.0)
    private val korea = OfflineBounds(west = 124.0, south = 33.0, east = 132.0, north = 39.0)

    @Test
    fun anyBoundsCoverASingleTileAtZoomZero() {
        Assert.assertEquals(1L, OfflineRegionEstimates.tileCount(paris, 0))
        Assert.assertEquals(1L, OfflineRegionEstimates.tileCount(korea, 0))
    }

    @Test
    fun aPointIsASingleTile() {
        val point = OfflineBounds(west = 0.0, south = 0.0, east = 0.0, north = 0.0)

        Assert.assertEquals(1L, OfflineRegionEstimates.tileCount(point, 10))
    }

    @Test
    fun wholeWorldIsEveryTileAtTheZoom() {
        val world = OfflineBounds(west = -179.9, south = -85.0, east = 179.9, north = 85.0)

        Assert.assertEquals(16L, OfflineRegionEstimates.tileCount(world, 2))
    }

    @Test
    fun latitudeIsClampedToTheMercatorLimit() {
        val beyondPole = OfflineBounds(west = 0.0, south = 89.0, east = 1.0, north = 90.0)

        // Both latitude edges collapse onto the top row, so only longitude widens.
        val expected = OfflineRegionEstimates.tileCount(
            OfflineBounds(west = 0.0, south = 85.0, east = 1.0, north = 85.0),
            8,
        )
        Assert.assertEquals(expected, OfflineRegionEstimates.tileCount(beyondPole, 8))
    }

    @Test
    fun parisTileCountMatchesWebMercator() {
        Assert.assertEquals(234L, OfflineRegionEstimates.tileCount(paris, 12))
    }

    @Test
    fun estimateGrowsWithMaximumZoom() {
        val low = OfflineRegionEstimates.estimatedBytes(paris, 0, 10)
        val high = OfflineRegionEstimates.estimatedBytes(paris, 0, 13)

        Assert.assertTrue(high > low)
    }

    @Test
    fun aCountryIsCappedBelowTheHighestSelectableZoom() {
        val maxZoom = OfflineRegionEstimates.maxSelectableZoom(korea)

        Assert.assertTrue(maxZoom < OfflineRegionEstimates.MAX_SELECTABLE_MAX_ZOOM)
        Assert.assertTrue(maxZoom >= OfflineRegionEstimates.MIN_SELECTABLE_MAX_ZOOM)
    }

    @Test
    fun theDefaultZoomNeverExceedsTheSelectableMaximum() {
        for (bounds in listOf(paris, korea, OfflineBounds(0.0, 0.0, 0.01, 0.01))) {
            val maxZoom = OfflineRegionEstimates.maxSelectableZoom(bounds)
            val defaultZoom = OfflineRegionEstimates.defaultMaxZoom(bounds)

            Assert.assertTrue(defaultZoom <= maxZoom)
            Assert.assertTrue(defaultZoom >= OfflineRegionEstimates.MIN_SELECTABLE_MAX_ZOOM)
        }
    }

    @Test
    fun aTinyAreaGetsTheHighestSelectableZoom() {
        val tiny = OfflineBounds(west = 0.0, south = 0.0, east = 0.01, north = 0.01)

        Assert.assertEquals(
            OfflineRegionEstimates.MAX_SELECTABLE_MAX_ZOOM,
            OfflineRegionEstimates.maxSelectableZoom(tiny),
        )
    }

    @Test
    fun ordinaryAreasAreWithinTheDownloadLimit() {
        Assert.assertTrue(OfflineRegionEstimates.isWithinLimit(paris))
        Assert.assertTrue(OfflineRegionEstimates.isWithinLimit(korea))
    }

    @Test
    fun aWorldSizedAreaIsRefused() {
        val world = OfflineBounds(west = -180.0, south = -85.0, east = 180.0, north = 85.0)

        Assert.assertFalse(OfflineRegionEstimates.isWithinLimit(world))
    }
}
