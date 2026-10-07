package org.btcmap.map

import org.btcmap.platform.PlatformLock
import org.btcmap.platform.withLock

/**
 * The number of features a viewport cache keeps before it drops the ones the
 * user has panned away from. Queries hit the local database, so rebuilding the
 * set for the current viewport is cheap compared to keeping an unbounded
 * history for the whole session.
 */
const val MAX_CACHED_FEATURES = 5_000

/**
 * Accumulates the features seen so far, keyed by [idOf], so panning back over
 * an already-loaded area never refetches it.
 *
 * The set is bounded: once a merge would push it past [maxItems] it is reset to
 * just the features of the current query. Resetting only loses work that a
 * later pan redoes, and it keeps a long session from growing without limit.
 * All access is synchronized because the SQLite fetch and the GeoJSON build
 * happen on different coroutine dispatchers.
 */
class FeatureStore<T>(
    private val idOf: (T) -> Long,
    private val maxItems: Int = MAX_CACHED_FEATURES,
) {
    private val lock = PlatformLock()
    private val seenIds = mutableSetOf<Long>()
    private val items = mutableSetOf<T>()

    /** The retained features, or null when [fetched] added nothing new. */
    fun merge(fetched: Collection<T>): Set<T>? = lock.withLock {
        if (items.size + fetched.size > maxItems) {
            seenIds.clear()
            items.clear()
        }

        val newOnes = fetched.filter { idOf(it) !in seenIds }
        if (newOnes.isEmpty()) return@withLock null

        seenIds.addAll(newOnes.map(idOf))
        items.addAll(newOnes)
        items.toSet()
    }

    fun clear() {
        lock.withLock {
            seenIds.clear()
            items.clear()
        }
    }

    /** The retained features, whether or not the last [merge] added anything. */
    fun snapshot(): Set<T> = lock.withLock { items.toSet() }

    val size: Int
        get() = lock.withLock { items.size }
}
