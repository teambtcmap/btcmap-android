package org.btcmap.util

import okhttp3.HttpUrl

private const val BTCMAP_HOST = "btcmap.org"
private const val MERCHANT_PATH_SEGMENT = "merchant"

fun HttpUrl.deepLinkPlaceId(): Long? {
    if (host != BTCMAP_HOST) return null

    val segments = pathSegments.filter { it.isNotEmpty() }
    if (segments.size != 2) return null
    if (segments[0] != MERCHANT_PATH_SEGMENT) return null

    return segments[1].toLongOrNull()
}
