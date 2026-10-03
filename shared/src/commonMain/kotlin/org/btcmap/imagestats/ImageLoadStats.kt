package org.btcmap.imagestats

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

/**
 * Where a successful image load was served from.
 *
 * The image library's own source enum is folded onto this one by the host, so
 * the counters do not depend on the image library.
 */
enum class ImageLoadSource {
    Memory,
    Disk,
    Network,
}

/**
 * Process-wide counters for image loads.
 *
 * The image pipeline is otherwise opaque, so the host's listener records every
 * load here and the image stats screen reads the totals. The counters are
 * updated from decoder and fetcher threads, hence the atomics.
 */
object ImageLoadStats {

    private val requests = atomic(0L)
    private val memoryCacheHits = atomic(0L)
    private val diskCacheHits = atomic(0L)
    private val networkLoads = atomic(0L)
    private val errors = atomic(0L)
    private val cancels = atomic(0L)
    private val successNanos = atomic(0L)
    private val successfulLoads = atomic(0L)

    /** Records that a load started. */
    fun recordStart() {
        requests.incrementAndGet()
    }

    /**
     * Records a successful load, bucketed by the source that served it.
     *
     * Only successes feed the average load time: errors and cancels are not
     * what it is meant to describe, and a cancelled load is often short, so
     * counting it would pull the average down misleadingly.
     */
    fun recordSuccess(source: ImageLoadSource, durationNanos: Long) {
        when (source) {
            ImageLoadSource.Memory -> memoryCacheHits.incrementAndGet()
            ImageLoadSource.Disk -> diskCacheHits.incrementAndGet()
            ImageLoadSource.Network -> networkLoads.incrementAndGet()
        }
        // A non-positive duration means the listener never saw the load start,
        // so there is nothing to average; skip it rather than count an instant.
        if (durationNanos > 0) {
            successNanos.addAndGet(durationNanos)
            successfulLoads.incrementAndGet()
        }
    }

    /** Records a load that failed. */
    fun recordError() {
        errors.incrementAndGet()
    }

    /** Records a load that was cancelled before it finished. */
    fun recordCancel() {
        cancels.incrementAndGet()
    }

    /** Reads the current totals. */
    fun snapshot(): ImageLoadCounters {
        val loads = successfulLoads.value
        return ImageLoadCounters(
            requests = requests.value,
            memoryCacheHits = memoryCacheHits.value,
            diskCacheHits = diskCacheHits.value,
            networkLoads = networkLoads.value,
            errors = errors.value,
            cancels = cancels.value,
            averageLoadMillis = if (loads == 0L) {
                0L
            } else {
                successNanos.value / loads / 1_000_000L
            },
        )
    }

    /** Clears every counter, for tests. */
    fun reset() {
        requests.value = 0
        memoryCacheHits.value = 0
        diskCacheHits.value = 0
        networkLoads.value = 0
        errors.value = 0
        cancels.value = 0
        successNanos.value = 0
        successfulLoads.value = 0
    }
}
