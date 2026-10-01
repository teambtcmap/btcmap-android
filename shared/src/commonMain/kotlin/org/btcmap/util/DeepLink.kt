package org.btcmap.util

import okhttp3.HttpUrl

private const val BTCMAP_HOST = "btcmap.org"
private const val MERCHANT_PATH_SEGMENT = "merchant"
private const val EVENT_PATH_SEGMENT = "event"

sealed interface DeepLink {
    data class Place(val id: Long) : DeepLink
    data class Event(val id: Long) : DeepLink
}

fun HttpUrl.deepLink(): DeepLink? {
    if (host != BTCMAP_HOST) return null

    val segments = pathSegments.filter { it.isNotEmpty() }
    if (segments.size != 2) return null

    val id = segments[1].toLongOrNull() ?: return null

    return when (segments[0]) {
        MERCHANT_PATH_SEGMENT -> DeepLink.Place(id)
        EVENT_PATH_SEGMENT -> DeepLink.Event(id)
        else -> null
    }
}
