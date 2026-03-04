package org.btcmap.api

import org.btcmap.http.httpClient
import org.btcmap.json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.settings.apiUrlV4
import org.btcmap.settings.prefs
import java.io.InputStream

data class GetAreasItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val websiteUrl: String,
)

object AreasApi {
    private const val ENDPOINT = "areas"

    fun InputStream.toAreas(): List<GetAreasItem> {
        return toJsonArray().map {
            GetAreasItem(
                id = it.getLong("id"),
                name = it.getString("name"),
                type = it.getString("type"),
                urlAlias = it.getString("url_alias"),
                icon = it.optString("icon").ifBlank { null },
                websiteUrl = it.getString("website_url"),
            )
        }
    }

    suspend fun getAreas(lat: Double, lon: Double): List<GetAreasItem> {
        val url = prefs.apiUrlV4(ENDPOINT).newBuilder().apply {
            addQueryParameter("lat", lat.toString())
            addQueryParameter("lon", lon.toString())
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toAreas() }
        }
    }
}
