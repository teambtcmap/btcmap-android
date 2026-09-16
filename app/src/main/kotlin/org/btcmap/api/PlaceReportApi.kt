package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.util.toJsonObject

data class ReportPlaceResponse(
    val id: Long,
    val origin: String,
)

suspend fun Api.reportPlace(placeId: Long, type: String, comment: String?): ReportPlaceResponse {
    val url = url.newBuilder().addPathSegments("v4/place-reports").build()

    val req = JsonObject().apply {
        addProperty("place_id", placeId)
        addProperty("type", type)
        val extra = JsonObject().apply {
            comment?.takeIf { it.isNotBlank() }?.let { addProperty("comment", it) }
        }
        if (extra.size() > 0) {
            add("extra_fields", extra)
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
        )
    }
}
