package org.btcmap.api

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.Request
import org.btcmap.util.toJsonObject
import java.util.Base64

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
                        put("data_base64", Base64.getEncoder().encodeToString(photo))
                    })
                }
            })
        }
    }

    return call(
        Request.Builder()
            .post(jsonBody(req))
            .url(url)
            .build()
    ) { stream ->
        val body = stream.toJsonObject()

        ReportPlaceResponse(
            id = body.long("id"),
            origin = body.string("origin"),
            photoIds = body.arrayOrNull("photo_ids")?.map { it.jsonPrimitive.long }.orEmpty(),
        )
    }
}
