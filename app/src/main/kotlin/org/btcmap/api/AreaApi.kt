package org.btcmap.api

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.util.Locale

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
    val url = pathBuilder("v4", "areas").apply {
        addQueryParameter("lat", lat.toString())
        addQueryParameter("lon", lon.toString())
    }.build()

    return call(Request.Builder().url(url).build()) { it.toAreas() }
}

suspend fun Api.getArea(
    id: String,
    lang: String = Locale.getDefault().language,
): GetAreaItem {
    val url = pathBuilder("v4", "areas", id).apply {
        if (lang.isNotBlank()) addQueryParameter("lang", lang)
    }.build()

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()
        GetAreaItem(
            id = body.long("id"),
            name = body.string("name"),
            type = body.string("type"),
            urlAlias = body.string("url_alias"),
            icon = body.nonBlankStringOrNull("icon"),
            iconWide = body.nonBlankStringOrNull("icon_wide"),
            websiteUrl = body.string("website_url"),
            description = body.nonBlankStringOrNull("description"),
        )
    }
}

suspend fun Api.saveArea(id: Long): List<Long> {
    val url = buildUrl("v4", "areas", "saved")
    val body = JsonPrimitive(id).toString().toRequestBody("application/json".toMediaType())

    return call(Request.Builder().post(body).url(url).build()) { it.toJsonLongArray() }
}

suspend fun Api.removeSavedArea(id: Long): List<Long> {
    val url = buildUrl("v4", "areas", "saved", "$id")

    return call(Request.Builder().delete().url(url).build()) { it.toJsonLongArray() }
}

private fun InputStream.toAreas(): List<GetAreasItem> {
    return toJsonArray().map { item ->
        val events = item.arrayOrNull("upcoming_events")
            ?.map { it.asJsonObject.toGetEventsItem() }
            ?: emptyList()

        GetAreasItem(
            id = item.long("id"),
            name = item.string("name"),
            type = item.string("type"),
            urlAlias = item.string("url_alias"),
            websiteUrl = item.string("website_url"),
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
