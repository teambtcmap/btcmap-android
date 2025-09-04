package api

import element_comment.ElementCommentJson
import element_comment.toElementCommentsJson
import json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.brotli.BrotliInterceptor
import okhttp3.coroutines.executeAsync
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val BASE_URL = "https://api.btcmap.org".toHttpUrl()

data class GetEventsItem(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
)

data class GetPlacesItem(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
    val updatedAt: String,
    val deletedAt: String?,
    val requiredAppUrl: String?,
    val boostedUntil: String?,
    val verifiedAt: String?,
    val address: String?,
    val openingHours: String?,
    val website: String?,
    val phone: String?,
    val email: String?,
    val twitter: String?,
    val facebook: String?,
    val instagram: String?,
    val line: String?,
    val bundled: Boolean,
    val comments: Long?,
)

private fun JSONObject.toGetPlacesItem(): GetPlacesItem {
    return GetPlacesItem(
        id = getLong("id"),
        lat = getDouble("lat"),
        lon = getDouble("lon"),
        icon = getString("icon"),
        name = getString("name"),
        updatedAt = getString("updated_at"),
        deletedAt = optString("deleted_at").ifBlank { null },
        requiredAppUrl = optString("required_app_url").ifBlank { null },
        boostedUntil = optString("boosted_until").ifBlank { null },
        verifiedAt = optString("verified_at").ifBlank { null },
        address = optString("verified_at").ifBlank { null },
        openingHours = optString("opening_hours").ifBlank { null },
        website = optString("website").ifBlank { null },
        phone = optString("phone").ifBlank { null },
        email = optString("email").ifBlank { null },
        twitter = optString("twitter").ifBlank { null },
        facebook = optString("facebook").ifBlank { null },
        instagram = optString("instagram").ifBlank { null },
        line = optString("line").ifBlank { null },
        bundled = false,
        comments = optLong("comments", 0),
    )
}

interface Api {
    suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<GetPlacesItem>

    suspend fun getElementComments(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<ElementCommentJson>

    suspend fun getEvents(): List<GetEventsItem>
}

class ApiImpl(
    private val baseUrl: HttpUrl = BASE_URL,
    private val httpClient: OkHttpClient = apiHttpClient(),
) : Api {
    override suspend fun getEvents(): List<GetEventsItem> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v4")
            addPathSegment("events")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { stream ->
                stream.toJsonArray().map {
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
        }
    }

    override suspend fun getElementComments(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<ElementCommentJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v4")
            addPathSegment("place-comments")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toElementCommentsJson() }
        }
    }

    override suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<GetPlacesItem> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v4")
            addPathSegment("places")
            addQueryParameter(
                "fields",
                "lat,lon,icon,name,updated_at,deleted_at,required_app_url,boosted_until,verified_at,address,opening_hours,website,phone,email,twitter,facebook,instagram,line,comments"
            )
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetPlacesItems() }
        }
    }
}

fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
    return toJsonArray().map { it.toGetPlacesItem() }
}

private fun apiHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(BrotliInterceptor)
        .addInterceptor {
            var res = it.proceed(it.request())

            var retryAttempts = 0

            while (res.code == 429 && retryAttempts < 10) {
                res.close()
                Thread.sleep(retryAttempts * 1000 + (Math.random() * 1000.0).toLong())
                res = it.proceed(it.request())
                retryAttempts++
            }

            res
        }.build()
}

private fun ZonedDateTime?.apiFormat(): String {
    return this?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: "2020-01-01T00:00:00Z"
}