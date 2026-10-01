package org.btcmap.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
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

    val extra = JsonObject().apply {
        addProperty("origin", userAgent)
        comment?.takeIf { it.isNotBlank() }?.let { addProperty("comment", it) }
    }

    val req = JsonObject().apply {
        addProperty("place_id", placeId)
        addProperty("type", type)
        add("extra_fields", extra)
        if (photos.isNotEmpty()) {
            add("photos", JsonArray().apply {
                photos.forEach { photo ->
                    add(JsonObject().apply {
                        addProperty("data_base64", Base64.getEncoder().encodeToString(photo))
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
            photoIds = body.arrayOrNull("photo_ids")?.map { it.asLong }.orEmpty(),
        )
    }
}
