package org.btcmap.imagestats

/**
 * A point-in-time view of [ImageLoadStats].
 *
 * The counters are lifetime counters: they cover every load since the process
 * started and are not persisted across restarts.
 */
data class ImageLoadCounters(
    val requests: Long,
    val memoryCacheHits: Long,
    val diskCacheHits: Long,
    val networkLoads: Long,
    val errors: Long,
    val cancels: Long,
    val averageLoadMillis: Long,
) {

    /**
     * The share of completed loads served without touching the network, or null
     * when nothing has completed yet.
     */
    val cacheHitRatePercent: Int?
        get() {
            val served = memoryCacheHits + diskCacheHits + networkLoads
            if (served == 0L) return null
            return ((memoryCacheHits + diskCacheHits) * 100 / served).toInt()
        }
}
