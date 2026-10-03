package org.btcmap.map

import kotlin.time.Instant
import org.btcmap.db.table.area.AreaGeometry

/**
 * An LRU of parsed area geometries.
 *
 * Bounded both by entry count and by the total number of points, because a
 * country's polygon can be orders of magnitude larger than a community's, so a
 * count cap alone cannot bound the memory. The most recently used entry is
 * always kept even when it alone exceeds [maxPoints], so the area under the map
 * centre is never evicted by the act of caching it.
 *
 * The common `LinkedHashMap` has no access-order mode, so a hit is reinserted to
 * move it to the end and the eldest (first) entry is evicted.
 */
internal class AreaGeometryCache(
    private val maxEntries: Int,
    private val maxPoints: Int,
) {
    private val entries = LinkedHashMap<Long, CachedGeometry>()
    private var totalPoints = 0

    val size: Int
        get() = entries.size

    /**
     * Returns the cached geometry for [id] when it was parsed from [updatedAt],
     * parsing and caching it with [parse] otherwise. Touching a hit marks the
     * entry as most recently used.
     */
    fun getOrPut(id: Long, updatedAt: Instant, parse: () -> AreaGeometry): AreaGeometry {
        val cached = entries[id]
        if (cached != null && cached.updatedAt == updatedAt) {
            entries.remove(id)
            entries[id] = cached
            return cached.geometry
        }

        val geometry = parse()
        entries.remove(id)?.let { totalPoints -= it.geometry.pointCount }
        entries[id] = CachedGeometry(updatedAt, geometry)
        totalPoints += geometry.pointCount
        evictToBudget()
        return geometry
    }

    private fun evictToBudget() {
        // Entries iterate least-recently used first, so the eldest are dropped.
        while ((entries.size > maxEntries || totalPoints > maxPoints) && entries.size > 1) {
            val eldest = entries.keys.first()
            totalPoints -= entries.remove(eldest)!!.geometry.pointCount
        }
    }

    private class CachedGeometry(
        val updatedAt: Instant,
        val geometry: AreaGeometry,
    )
}
