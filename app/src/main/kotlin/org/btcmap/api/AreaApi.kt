package org.btcmap.api

import com.google.gson.JsonPrimitive
import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonLongArray
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
    val url = buildUrl("v4", "areas") {
        addQueryParameter("lat", lat.toString())
        addQueryParameter("lon", lon.toString())
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toAreas() }
}

suspend fun Api.getArea(
    id: String,
    lang: String = Locale.getDefault().language,
): GetAreaItem {
    val url = buildUrl("v4", "areas", id) {
        if (lang.isNotBlank()) addQueryParameter("lang", lang)
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { stream ->
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
    val body = jsonBody(JsonPrimitive(id))

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
