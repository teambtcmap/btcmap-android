package org.btcmap.imagestats

import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult

/**
 * Records every Coil load into [ImageLoadStats].
 *
 * The factory creates one listener per request, so [startNanos] belongs to a
 * single load and no request correlation is needed.
 */
class ImageStatsEventListener : EventListener() {

    private var startNanos = 0L

    override fun onStart(request: ImageRequest) {
        startNanos = System.nanoTime()
        ImageLoadStats.recordStart()
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        ImageLoadStats.recordSuccess(result.dataSource, durationNanos())
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        ImageLoadStats.recordError(durationNanos())
    }

    override fun onCancel(request: ImageRequest) {
        ImageLoadStats.recordCancel(durationNanos())
    }

    private fun durationNanos(): Long {
        val start = startNanos
        return if (start == 0L) 0L else System.nanoTime() - start
    }

    companion object Factory : EventListener.Factory {
        override fun create(request: ImageRequest): EventListener = ImageStatsEventListener()
    }
}
