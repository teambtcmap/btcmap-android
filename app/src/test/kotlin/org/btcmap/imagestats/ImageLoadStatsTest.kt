package org.btcmap.imagestats

import coil3.decode.DataSource
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ImageLoadStatsTest {

    @Before
    fun setUp() {
        ImageLoadStats.reset()
    }

    @Test
    fun snapshot_startsEmpty() {
        val counters = ImageLoadStats.snapshot()
        Assert.assertEquals(0L, counters.requests)
        Assert.assertEquals(0L, counters.memoryCacheHits)
        Assert.assertEquals(0L, counters.diskCacheHits)
        Assert.assertEquals(0L, counters.networkLoads)
        Assert.assertNull(counters.cacheHitRatePercent)
    }

    @Test
    fun recordStart_countsRequests() {
        ImageLoadStats.recordStart()
        ImageLoadStats.recordStart()

        Assert.assertEquals(2L, ImageLoadStats.snapshot().requests)
    }

    @Test
    fun recordSuccess_bucketsByDataSource() {
        ImageLoadStats.recordSuccess(DataSource.MEMORY_CACHE, 0)
        ImageLoadStats.recordSuccess(DataSource.MEMORY, 0)
        ImageLoadStats.recordSuccess(DataSource.DISK, 0)
        ImageLoadStats.recordSuccess(DataSource.NETWORK, 0)

        val counters = ImageLoadStats.snapshot()
        Assert.assertEquals(2L, counters.memoryCacheHits)
        Assert.assertEquals(1L, counters.diskCacheHits)
        Assert.assertEquals(1L, counters.networkLoads)
    }

    @Test
    fun recordError_andCancel_areCounted() {
        ImageLoadStats.recordError(0)
        ImageLoadStats.recordCancel(0)

        val counters = ImageLoadStats.snapshot()
        Assert.assertEquals(1L, counters.errors)
        Assert.assertEquals(1L, counters.cancels)
    }

    @Test
    fun snapshot_averagesLoadDuration() {
        val tenMillis = TimeUnit.MILLISECONDS.toNanos(10)
        ImageLoadStats.recordSuccess(DataSource.NETWORK, tenMillis)
        ImageLoadStats.recordSuccess(DataSource.NETWORK, tenMillis * 3)

        Assert.assertEquals(20L, ImageLoadStats.snapshot().averageLoadMillis)
    }

    @Test
    fun cacheHitRate_ignoresIncompleteLoads() {
        ImageLoadStats.recordStart()
        ImageLoadStats.recordSuccess(DataSource.MEMORY_CACHE, 0)
        ImageLoadStats.recordSuccess(DataSource.NETWORK, 0)

        Assert.assertEquals(50, ImageLoadStats.snapshot().cacheHitRatePercent)
    }

    @Test
    fun cacheHitRate_isNullWhenNothingCompleted() {
        ImageLoadStats.recordStart()

        Assert.assertNull(ImageLoadStats.snapshot().cacheHitRatePercent)
    }

    @Test
    fun reset_clearsCounters() {
        ImageLoadStats.recordStart()
        ImageLoadStats.recordSuccess(DataSource.NETWORK, 1)

        ImageLoadStats.reset()

        val counters = ImageLoadStats.snapshot()
        Assert.assertEquals(0L, counters.requests)
        Assert.assertEquals(0L, counters.networkLoads)
        Assert.assertEquals(0L, counters.averageLoadMillis)
    }
}
