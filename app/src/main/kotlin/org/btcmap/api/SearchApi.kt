package org.btcmap.api

import okhttp3.Request
import org.btcmap.util.toJsonObject
import java.io.InputStream

sealed class SearchResult {
    data class Area(
        val id: Long,
        val name: String,
        val bbox: List<Double>?,
        val iconUrl: String?,
    ) : SearchResult()

    data class Place(
        val id: Long,
        val name: String,
        val icon: String,
        val lat: Double,
        val lon: Double,
    ) : SearchResult()
}

suspend fun Api.search(
    query: String,
    lat: Double?,
    lon: Double?,
    limit: Long = 20,
): List<SearchResult> {
    val url = url.newBuilder().addPathSegments("v4/search/").apply {
        addQueryParameter("q", query)
        if (lat != null && lon != null) {
            addQueryParameter("lat", lat.toString())
            addQueryParameter("lon", lon.toString())
        }
        addQueryParameter("limit", "$limit")
    }.build()

    return call(Request.Builder().url(url).build()) { it.toSearchResults() }
}

private fun InputStream.toSearchResults(): List<SearchResult> {
    val body = toJsonObject()
    val results = body.getAsJsonArray("results") ?: return emptyList()

    return results.mapNotNull { element ->
        val item = element.asJsonObject
        when (item.get("type").asString) {
            "area" -> SearchResult.Area(
                id = item.get("id").asLong,
                name = item.get("name").asString,
                bbox = if (!item.has("bbox") || item.get("bbox").isJsonNull) {
                    null
                } else {
                    item.getAsJsonArray("bbox").map { it.asDouble }
                },
                iconUrl = if (!item.has("icon") || item.get("icon").isJsonNull) {
                    null
                } else {
                    item.get("icon").asString
                },
            )

            "place" -> SearchResult.Place(
                id = item.get("id").asLong,
                name = item.get("name").asString,
                icon = item.get("icon").asString,
                lat = item.get("lat").asDouble,
                lon = item.get("lon").asDouble,
            )

            else -> null
        }
    }
}
