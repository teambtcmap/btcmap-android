package org.btcmap.api

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

    val req = buildJsonObject {
        put("lat", lat)
        put("lon", lon)
        put("category", category)
        put("name", name)
        val extra = buildJsonObject {
            put("origin", userAgent)
            address?.takeIf { it.isNotBlank() }?.let { put("address", it) }
            website?.takeIf { it.isNotBlank() }?.let { put("website", it) }
            description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
        }
        put("extra_fields", extra)
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
