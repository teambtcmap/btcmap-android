package org.btcmap.imagestats

import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import java.text.NumberFormat

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
    val numberFormatter = NumberFormat.getIntegerInstance()
    val sections = mutableListOf<StatsSection>()

    cache.memory?.let { memory ->
        sections.add(
            StatsSection(
                key = "memory",
                title = labels.memoryCache,
                icon = "memory",
                entries = listOf(
                    StatsEntry(labels.entries, numberFormatter.format(memory.entryCount)),
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
                add(StatsEntry(labels.requests, numberFormatter.format(counters.requests)))
                add(StatsEntry(labels.memoryHits, numberFormatter.format(counters.memoryCacheHits)))
                add(StatsEntry(labels.diskHits, numberFormatter.format(counters.diskCacheHits)))
                add(StatsEntry(labels.networkLoads, numberFormatter.format(counters.networkLoads)))
                counters.cacheHitRatePercent?.let {
                    add(StatsEntry(labels.cacheHitRate, labels.percent(it)))
                }
                add(StatsEntry(labels.errors, numberFormatter.format(counters.errors)))
                add(StatsEntry(labels.cancels, numberFormatter.format(counters.cancels)))
                add(StatsEntry(labels.averageLoad, labels.millis(counters.averageLoadMillis)))
            },
        )
    )

    return sections
}
