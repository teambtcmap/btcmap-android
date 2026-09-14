package org.btcmap.api

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream

data class GetAreasItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val websiteUrl: String,
    val upcomingEventsCount: Int,
    val upcomingEvents: List<GetEventsItem>,
)

data class GetAreaItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val iconWide: String?,
    val websiteUrl: String,
    val description: String?,
)

suspend fun Api.getAreas(lat: Double, lon: Double): List<GetAreasItem> {
    val url = url.newBuilder().addPathSegments("v4/areas").apply {
        addQueryParameter("lat", lat.toString())
        addQueryParameter("lon", lon.toString())
    }.build()

    return call(Request.Builder().url(url).build()) { it.toAreas() }
}

suspend fun Api.getArea(id: String): GetAreaItem {
    val url = url.newBuilder().addPathSegments("v4/areas/$id").build()

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()
        GetAreaItem(
            id = body.get("id").asLong,
            name = body.get("name").asString,
            type = body.get("type").asString,
            urlAlias = body.get("url_alias").asString,
            icon = if (!body.has("icon") || body.get("icon").isJsonNull) null else body.get(
                "icon"
            ).asString.ifBlank { null },
            iconWide = if (!body.has("icon_wide") || body.get("icon_wide").isJsonNull) null else body.get(
                "icon_wide"
            ).asString.ifBlank { null },
            websiteUrl = body.get("website_url").asString,
            description = if (!body.has("description") || body.get("description").isJsonNull) null else body.get(
                "description"
            ).asString.ifBlank { null },
        )
    }
}

suspend fun Api.saveArea(id: Long): List<Long> {
    val url = url.newBuilder().addPathSegments("v4/areas/saved").build()
    val body = JsonPrimitive(id).toString().toRequestBody("application/json".toMediaType())

    return call(Request.Builder().post(body).url(url).build()) { it.toJsonLongArray() }
}

suspend fun Api.removeSavedArea(id: Long): List<Long> {
    val url = url.newBuilder().addPathSegments("v4/areas/saved/$id").build()

    return call(Request.Builder().delete().url(url).build()) { it.toJsonLongArray() }
}

private fun InputStream.toAreas(): List<GetAreasItem> {
    return toJsonArray().map { element ->
        val item = element.asJsonObject
        val events = if (item.has("upcoming_events") && !item.get("upcoming_events").isJsonNull) {
            item.getAsJsonArray("upcoming_events").map { it.asJsonObject.toGetEventsItem() }
        } else {
            emptyList()
        }

        GetAreasItem(
            id = item.get("id").asLong,
            name = item.get("name").asString,
            type = item.get("type").asString,
            urlAlias = item.get("url_alias").asString,
            websiteUrl = item.get("website_url").asString,
            upcomingEventsCount = events.size,
            upcomingEvents = events,
        )
    }
}

internal fun InputStream.toJsonLongArray(): List<Long> {
    val rawJson = bufferedReader().use { it.readText() }
    val jsonArray = JsonParser.parseString(rawJson).asJsonArray

    return List(jsonArray.size()) { jsonArray.get(it).asLong }
}
