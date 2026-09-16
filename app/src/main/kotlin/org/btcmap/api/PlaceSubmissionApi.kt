package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.util.toJsonObject

data class SubmitPlaceResponse(
    val id: Long,
)

suspend fun Api.submitPlace(
    lat: Double,
    lon: Double,
    category: String,
    name: String,
    address: String?,
    website: String?,
    description: String?,
): SubmitPlaceResponse {
    val url = buildUrl("v4", "place-submissions")

    val req = JsonObject().apply {
        addProperty("lat", lat)
        addProperty("lon", lon)
        addProperty("category", category)
        addProperty("name", name)
        val extra = JsonObject().apply {
            address?.takeIf { it.isNotBlank() }?.let { addProperty("address", it) }
            website?.takeIf { it.isNotBlank() }?.let { addProperty("website", it) }
            description?.takeIf { it.isNotBlank() }?.let { addProperty("description", it) }
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

        SubmitPlaceResponse(id = body.long("id"))
    }
}
