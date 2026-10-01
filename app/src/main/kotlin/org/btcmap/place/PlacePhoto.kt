package org.btcmap.place

/**
 * A place image prepared for the gallery: a small URL for the thumbnail strip
 * and a larger one for the fullscreen viewer.
 */
data class PlacePhoto(
    val id: Long,
    val thumbnailUrl: String,
    val fullUrl: String,
)
