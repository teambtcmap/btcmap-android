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
    val url = buildUrl("v4", "search") {
        addQueryParameter("q", query)
        if (lat != null && lon != null) {
            addQueryParameter("lat", lat.toString())
            addQueryParameter("lon", lon.toString())
        }
        addQueryParameter("limit", "$limit")
    }

    return call(Request.Builder().url(url).build()) { it.toSearchResults() }
}

private fun InputStream.toSearchResults(): List<SearchResult> {
    val body = toJsonObject()
    val results = body.arrayOrNull("results") ?: return emptyList()

    return results.mapNotNull { element ->
        val item = element.asJsonObject
        when (item.string("type")) {
            "area" -> SearchResult.Area(
                id = item.long("id"),
                name = item.string("name"),
                bbox = item.doubleArrayOrNull("bbox"),
                iconUrl = item.stringOrNull("icon"),
            )

            "place" -> SearchResult.Place(
                id = item.long("id"),
                name = item.string("name"),
                icon = item.string("icon"),
                lat = item.double("lat"),
                lon = item.double("lon"),
            )

            else -> null
        }
    }
}
