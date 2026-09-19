package org.btcmap.imagestats

import coil3.decode.DataSource
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide counters for image loads.
 *
 * Coil's image pipeline is otherwise opaque, so [ImageStatsEventListener]
 * records every load here and the image stats screen reads the totals. The
 * counters are updated from Coil's decoder and fetcher threads, hence the
 * atomics.
 */
object ImageLoadStats {

    private val requests = AtomicLong()
    private val memoryCacheHits = AtomicLong()
    private val diskCacheHits = AtomicLong()
    private val networkLoads = AtomicLong()
    private val errors = AtomicLong()
    private val cancels = AtomicLong()
    private val totalNanos = AtomicLong()
    private val completed = AtomicLong()

    /** Records that a load started. */
    fun recordStart() {
        requests.incrementAndGet()
    }

    /** Records a successful load, bucketed by the source that served it. */
    fun recordSuccess(dataSource: DataSource, durationNanos: Long) {
        when (dataSource) {
            // MEMORY covers images that were already decoded in memory outside
            // the cache; either way the load did not hit disk or the network.
            DataSource.MEMORY_CACHE, DataSource.MEMORY -> memoryCacheHits.incrementAndGet()
            DataSource.DISK -> diskCacheHits.incrementAndGet()
            DataSource.NETWORK -> networkLoads.incrementAndGet()
        }
        recordCompletion(durationNanos)
    }

    /** Records a load that failed. */
    fun recordError(durationNanos: Long) {
        errors.incrementAndGet()
        recordCompletion(durationNanos)
    }

    /** Records a load that was cancelled before it finished. */
    fun recordCancel(durationNanos: Long) {
        cancels.incrementAndGet()
        recordCompletion(durationNanos)
    }

    /** Reads the current totals. */
    fun snapshot(): ImageLoadCounters {
        val completedCount = completed.get()
        return ImageLoadCounters(
            requests = requests.get(),
            memoryCacheHits = memoryCacheHits.get(),
            diskCacheHits = diskCacheHits.get(),
            networkLoads = networkLoads.get(),
            errors = errors.get(),
            cancels = cancels.get(),
            averageLoadMillis = if (completedCount == 0L) {
                0L
            } else {
                TimeUnit.NANOSECONDS.toMillis(totalNanos.get() / completedCount)
            },
        )
    }

    /** Clears every counter, for tests. */
    fun reset() {
        requests.set(0)
        memoryCacheHits.set(0)
        diskCacheHits.set(0)
        networkLoads.set(0)
        errors.set(0)
        cancels.set(0)
        totalNanos.set(0)
        completed.set(0)
    }

    private fun recordCompletion(durationNanos: Long) {
        if (durationNanos > 0) {
            totalNanos.addAndGet(durationNanos)
        }
        completed.incrementAndGet()
    }
}
