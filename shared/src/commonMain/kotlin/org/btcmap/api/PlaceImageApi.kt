@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject

data class PlaceImage(
    val id: Long,
    val placeId: Long,
    val type: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val createdAt: String,
    val createdBy: Long?,
    /** The uploader's display name, when the API resolves an `author` for it. */
    val authorName: String?,
)

/** Lists the metadata of every image stored against a place, newest first. */
suspend fun Api.getPlaceImages(placeId: Long): List<PlaceImage> {
    val url = buildUrl("v4", "places", "$placeId", "images")

    return call(HttpMethod.Get, url, withoutAuth = true) { body ->
        body.toJsonArray().map { it.toPlaceImage() }
    }
}

/**
 * Uploads a photo for a place, attributed to the signed-in user. Requires a
 * Bearer token; the server stores it with `type = "user"`.
 */
suspend fun Api.addPlaceImage(placeId: Long, photo: ByteArray): PlaceImage {
    val url = buildUrl("v4", "places", "$placeId", "images")

    val req = buildJsonObject {
        put("data_base64", Base64.encode(photo))
    }

    return call(HttpMethod.Post, url, body = req) { body ->
        body.toJsonObject().toPlaceImage()
    }
}

/**
 * Lists the images the signed-in user uploaded, across all places, newest
 * first. Requires a Bearer token.
 */
suspend fun Api.getMyPlaceImages(): List<PlaceImage> {
    val url = buildUrl("v4", "users", "me", "place-images")

    return call(HttpMethod.Get, url) { body ->
        body.toJsonArray().map { it.toPlaceImage() }
    }
}

/**
 * Lists the most recently added place images across every place, newest first,
 * for moderation. Restricted to `admin` and `root` users; the server rejects
 * anyone else with 403. Defaults to the API's maximum of 100 images.
 */
suspend fun Api.getRecentPlaceImages(limit: Int = 100): List<PlaceImage> {
    val url = buildUrl("v4", "place-images") {
        parameters.append("limit", "$limit")
    }

    return call(HttpMethod.Get, url) { body ->
        body.toJsonArray().map { it.toPlaceImage() }
    }
}

/**
 * Deletes an image from a place. A regular user may only delete their own
 * uploads; the server rejects anyone else's with 403.
 */
suspend fun Api.deletePlaceImage(placeId: Long, imageId: Long): PlaceImage {
    val url = buildUrl("v4", "places", "$placeId", "images", "$imageId")

    return call(HttpMethod.Delete, url) { body ->
        body.toJsonObject().toPlaceImage()
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
        width?.let { parameters.append("w", "$it") }
        height?.let { parameters.append("h", "$it") }
    }.toString()
}

private fun JsonObject.toPlaceImage(): PlaceImage {
    return PlaceImage(
        id = long("id"),
        placeId = long("place_id"),
        type = string("type"),
        width = int("width"),
        height = int("height"),
        sizeBytes = long("size_bytes"),
        createdAt = string("created_at"),
        createdBy = longOrNull("created_by"),
        authorName = objectOrNull("author")?.nonBlankStringOrNull("name"),
    )
}
