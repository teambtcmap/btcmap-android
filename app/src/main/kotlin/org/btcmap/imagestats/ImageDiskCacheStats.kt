package org.btcmap.imagestats

/** Usage of Coil's on-disk image cache. */
data class ImageDiskCacheStats(
    val directory: String,
    val usedBytes: Long,
    val maxBytes: Long,
)
