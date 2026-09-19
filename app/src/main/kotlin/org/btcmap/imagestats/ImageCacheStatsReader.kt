package org.btcmap.imagestats

import coil3.ImageLoader
import org.btcmap.util.abbreviateHome

/**
 * Reads Coil's cache usage. Coil does not expose the number of disk cache
 * entries, only the disk cache's size against its maximum, so that is all the
 * stats screen can show for it.
 */
class ImageCacheStatsReader(
    private val imageLoader: ImageLoader,
    private val homeDirectory: String,
) {

    /** Reads the current memory and disk cache usage. */
    fun read(): ImageCacheStats {
        val memory = imageLoader.memoryCache?.let {
            ImageMemoryCacheStats(
                entryCount = it.keys.size,
                usedBytes = it.size,
                maxBytes = it.maxSize,
            )
        }

        val disk = imageLoader.diskCache?.let {
            ImageDiskCacheStats(
                directory = it.directory.toString().abbreviateHome(homeDirectory),
                usedBytes = it.size,
                maxBytes = it.maxSize,
            )
        }

        return ImageCacheStats(memory = memory, disk = disk)
    }
}
