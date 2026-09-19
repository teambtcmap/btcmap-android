package org.btcmap.area

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import org.btcmap.R

/**
 * Warms the area screen's header image so it is already decoded and cached by
 * the time the screen opens.
 *
 * The request uses the header's size and fill scale, matching how
 * [AreaFragment] loads the image, so the screen reuses this exact bitmap from
 * Coil's memory cache instead of returning to the network. Coil skips the work
 * when the image is already cached, so calling this for every row or chip that
 * becomes visible is cheap.
 */
fun Context.preloadAreaHeader(url: String?) {
    if (url.isNullOrBlank()) return

    val width = resources.displayMetrics.widthPixels
    val height = resources.getDimensionPixelSize(R.dimen.area_header_height)
    val request = ImageRequest.Builder(this)
        .data(url)
        .size(width, height)
        .scale(Scale.FILL)
        .build()
    SingletonImageLoader.get(this).enqueue(request)
}
