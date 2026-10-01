package org.btcmap.imagestats

/**
 * A snapshot of Coil's caches. Each field is null when the image loader was
 * built without that cache.
 */
data class ImageCacheStats(
    val memory: ImageMemoryCacheStats?,
    val disk: ImageDiskCacheStats?,
)
