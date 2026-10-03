@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlin.io.encoding.Base64
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonObject

data class ReportPlaceResponse(
    val id: Long,
    val origin: String,
    val photoIds: List<Long>,
)

suspend fun Api.reportPlace(
    placeId: Long,
    type: String,
    comment: String?,
    photos: List<ByteArray> = emptyList(),
): ReportPlaceResponse {
    val url = buildUrl("v4", "place-reports")

    val extra = buildJsonObject {
        put("origin", userAgent)
        comment?.takeIf { it.isNotBlank() }?.let { put("comment", it) }
    }

    val req = buildJsonObject {
        put("place_id", placeId)
        put("type", type)
        put("extra_fields", extra)
        if (photos.isNotEmpty()) {
            put("photos", buildJsonArray {
                photos.forEach { photo ->
                    add(buildJsonObject {
                        put("data_base64", Base64.encode(photo))
                    })
                }
            })
        }
    }

    return call(HttpMethod.Post, url, body = req) { body ->
        val parsed = body.toJsonObject()

        ReportPlaceResponse(
            id = parsed.long("id"),
            origin = parsed.string("origin"),
            photoIds = parsed.arrayOrNull("photo_ids")?.map { it.jsonPrimitive.long }.orEmpty(),
        )
    }
}
