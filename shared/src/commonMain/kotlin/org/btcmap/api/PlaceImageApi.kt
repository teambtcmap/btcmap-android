package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.util.Base64

data class PlaceImage(
    val id: Long,
    val placeId: Long,
    val type: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val createdAt: String,
    val createdBy: Long?,
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
        put("data_base64", Base64.getEncoder().encodeToString(photo))
    }

    return call(HttpMethod.Post, url, body = req) { body ->
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
    )
}
