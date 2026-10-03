package org.btcmap.imagestats

import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.platform.formatInteger

/** The per-host labels the image stats cards are built with. */
data class ImageStatsLabels(
    val memoryCache: String,
    val diskCache: String,
    val loads: String,
    val entries: String,
    val size: String,
    val location: String,
    val requests: String,
    val memoryHits: String,
    val diskHits: String,
    val networkLoads: String,
    val cacheHitRate: String,
    val errors: String,
    val cancels: String,
    val averageLoad: String,
    val percent: (value: Int) -> String,
    val millis: (value: Long) -> String,
    val sizeOf: (usedBytes: Long, maxBytes: Long) -> String,
)

/**
 * Builds the memory-cache, disk-cache and load-counter cards. The strings and
 * the byte/percent formatting stay with the host.
 */
fun imageStatsSections(
    cache: ImageCacheStats,
    counters: ImageLoadCounters,
    labels: ImageStatsLabels,
): List<StatsSection> {
    val sections = mutableListOf<StatsSection>()

    cache.memory?.let { memory ->
        sections.add(
            StatsSection(
                key = "memory",
                title = labels.memoryCache,
                icon = "memory",
                entries = listOf(
                    StatsEntry(labels.entries, formatInteger(memory.entryCount.toLong())),
                    StatsEntry(labels.size, labels.sizeOf(memory.usedBytes, memory.maxBytes)),
                ),
            )
        )
    }

    cache.disk?.let { disk ->
        sections.add(
            StatsSection(
                key = "disk",
                title = labels.diskCache,
                icon = "hard_drive",
                entries = listOf(
                    StatsEntry(labels.location, disk.directory),
                    StatsEntry(labels.size, labels.sizeOf(disk.usedBytes, disk.maxBytes)),
                ),
            )
        )
    }

    sections.add(
        StatsSection(
            key = "loads",
            title = labels.loads,
            icon = "query_stats",
            entries = buildList {
                add(StatsEntry(labels.requests, formatInteger(counters.requests)))
                add(StatsEntry(labels.memoryHits, formatInteger(counters.memoryCacheHits)))
                add(StatsEntry(labels.diskHits, formatInteger(counters.diskCacheHits)))
                add(StatsEntry(labels.networkLoads, formatInteger(counters.networkLoads)))
                counters.cacheHitRatePercent?.let {
                    add(StatsEntry(labels.cacheHitRate, labels.percent(it)))
                }
                add(StatsEntry(labels.errors, formatInteger(counters.errors)))
                add(StatsEntry(labels.cancels, formatInteger(counters.cancels)))
                add(StatsEntry(labels.averageLoad, labels.millis(counters.averageLoadMillis)))
            },
        )
    )

    return sections
}
