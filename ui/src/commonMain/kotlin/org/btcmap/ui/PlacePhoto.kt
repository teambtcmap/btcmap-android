package org.btcmap.ui

import org.btcmap.api.Api
import org.btcmap.api.PlaceImage
import org.btcmap.api.placeImageUrl

/**
 * One place photo as the gallery needs it: the small URL for the thumbnail
 * strip, the large URL for the full-screen viewer, and the uploader's name when
 * the API knows it. The endpoint resizes on the fly, so the two URLs ask for
 * only the dimensions each surface draws and never download the stored master.
 */
data class PlacePhoto(
    val imageId: Long,
    val placeId: Long,
    val thumbnailUrl: String,
    val fullUrl: String,
    val authorName: String? = null,
    /** Whether the signed-in user may delete this photo. Resolved by the host. */
    val canDelete: Boolean = false,
)

/** The pixel bound the strip's thumbnails are requested at. */
const val PLACE_PHOTO_THUMBNAIL_SIZE = 320

/**
 * The pixel bound the full-screen viewer requests. The API never upscales, so
 * this only caps a large master; a small photo is still served in full.
 */
const val PLACE_PHOTO_FULL_SIZE = 2048

/**
 * The gallery rendition of [image]: the two resized URLs the surfaces draw, the
 * uploader's name, and whether the signed-in user may delete it. Centralized so
 * every host requests the same sizes.
 */
fun Api.toPlacePhoto(image: PlaceImage, canDelete: Boolean = false): PlacePhoto = PlacePhoto(
    imageId = image.id,
    placeId = image.placeId,
    thumbnailUrl = placeImageUrl(
        placeId = image.placeId,
        imageId = image.id,
        width = PLACE_PHOTO_THUMBNAIL_SIZE,
        height = PLACE_PHOTO_THUMBNAIL_SIZE,
    ),
    fullUrl = placeImageUrl(
        placeId = image.placeId,
        imageId = image.id,
        width = PLACE_PHOTO_FULL_SIZE,
        height = PLACE_PHOTO_FULL_SIZE,
    ),
    authorName = image.authorName,
    canDelete = canDelete,
)
