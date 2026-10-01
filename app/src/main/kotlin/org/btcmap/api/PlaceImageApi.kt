package org.btcmap.api

import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.toJsonArray

data class PlaceImage(
    val id: Long,
    val placeId: Long,
    val type: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val createdAt: String,
)

/** Lists the metadata of every image stored against a place, newest first. */
suspend fun Api.getPlaceImages(placeId: Long): List<PlaceImage> {
    val url = buildUrl("v4", "places", "$placeId", "images")

    return call(Request.Builder().withoutAuth().url(url).build()) { stream ->
        stream.toJsonArray().map {
            PlaceImage(
                id = it.long("id"),
                placeId = it.long("place_id"),
                type = it.string("type"),
                width = it.int("width"),
                height = it.int("height"),
                sizeBytes = it.long("size_bytes"),
                createdAt = it.string("created_at"),
            )
        }
    }
}

/**
 * The public URL serving an image's bytes. The endpoint resizes on the fly, so
 * callers request only the dimensions they draw instead of the stored master.
 */
fun Api.placeImageUrl(
    placeId: Long,
    imageId: Long,
    width: Int? = null,
    height: Int? = null,
): String {
    return buildUrl("v4", "places", "$placeId", "images", "$imageId") {
        width?.let { addQueryParameter("w", "$it") }
        height?.let { addQueryParameter("h", "$it") }
    }.toString()
}
