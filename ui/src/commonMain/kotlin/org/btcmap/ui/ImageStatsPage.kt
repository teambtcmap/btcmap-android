package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.imagestats.ImageLoadStats
import org.btcmap.imagestats.ImageStatsLabels
import org.btcmap.imagestats.imageStatsSections
import org.btcmap.stats.StatsSection
import org.btcmap.util.rethrowIfCancellation

/**
 * The image cache and load-counter screen: it reads Coil's caches and the
 * process-wide load counters and renders them through the shared [StatsScreen].
 * [refreshKey] re-reads the snapshot.
 */
@Composable
fun ImageStatsPage(
    imageLoader: ImageLoader,
    homeDirectory: String,
    labels: ImageStatsLabels,
    refreshKey: Int = 0,
    onError: (Throwable) -> Unit = {},
) {
    var sections by remember { mutableStateOf<List<StatsSection>>(emptyList()) }

    LaunchedEffect(refreshKey) {
        try {
            // Reading the caches can initialize them and touch the disk, so
            // keep it off the main thread.
            val cache = withContext(Dispatchers.IO) {
                ImageCacheStatsReader(imageLoader, homeDirectory).read()
            }
            sections = imageStatsSections(cache, ImageLoadStats.snapshot(), labels)
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            onError(e)
        }
    }

    StatsScreen(sections = sections)
}
