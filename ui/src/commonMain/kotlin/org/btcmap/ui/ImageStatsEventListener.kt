package org.btcmap.ui

import coil3.EventListener
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import org.btcmap.imagestats.ImageLoadSource
import org.btcmap.imagestats.ImageLoadStats

/**
 * Records every Coil load into [ImageLoadStats].
 *
 * The factory creates one listener per request, so [startNanos] belongs to a
 * single load and no request correlation is needed.
 */
class ImageStatsEventListener : EventListener() {

    // Written in onStart and read from whichever thread finishes the load, so
    // mark it volatile to publish the start time across those threads.
    @Volatile
    private var startNanos = 0L

    override fun onStart(request: ImageRequest) {
        startNanos = System.nanoTime()
        ImageLoadStats.recordStart()
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        ImageLoadStats.recordSuccess(result.dataSource.toLoadSource(), durationNanos())
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        ImageLoadStats.recordError()
    }

    override fun onCancel(request: ImageRequest) {
        ImageLoadStats.recordCancel()
    }

    private fun durationNanos(): Long {
        val start = startNanos
        return if (start == 0L) 0L else System.nanoTime() - start
    }

    /**
     * Coil's source as the shared counter's bucket. `MEMORY` covers images that
     * were already decoded in memory outside the cache; either way the load did
     * not hit disk or the network.
     */
    private fun DataSource.toLoadSource(): ImageLoadSource = when (this) {
        DataSource.MEMORY_CACHE, DataSource.MEMORY -> ImageLoadSource.Memory
        DataSource.DISK -> ImageLoadSource.Disk
        DataSource.NETWORK -> ImageLoadSource.Network
    }

    companion object Factory : EventListener.Factory {
        override fun create(request: ImageRequest): EventListener = ImageStatsEventListener()
    }
}
