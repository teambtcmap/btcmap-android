package org.btcmap.imagestats

/** Usage of Coil's in-memory bitmap cache. */
data class ImageMemoryCacheStats(
    val entryCount: Int,
    val usedBytes: Long,
    val maxBytes: Long,
)
