package api

import json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.InputStream
import java.time.ZonedDateTime

object EventApi {

    private const val ENDPOINT = "events"

    data class GetEventsItem(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val name: String,
        val website: HttpUrl,
        val startsAt: ZonedDateTime,
        val endsAt: ZonedDateTime?,
    )

    private fun InputStream.toGetEventsItems(): List<GetEventsItem> {
        return toJsonArray().map {
            GetEventsItem(
                id = it.getLong("id"),
                lat = it.getDouble("lat"),
                lon = it.getDouble("lon"),
                name = it.getString("name"),
                website = it.getString("website").toHttpUrl(),
                startsAt = ZonedDateTime.parse(it.getString("starts_at")),
                endsAt = if (it.isNull("ends_at")) null else ZonedDateTime.parse(
                    it.getString(
                        "ends_at"
                    )
                )
            )
        }
    }

    suspend fun getEvents(): List<GetEventsItem> {
        val url = apiUrl(ENDPOINT).build()

        val res = apiHttpClient().newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetEventsItems() }
        }
    }
}