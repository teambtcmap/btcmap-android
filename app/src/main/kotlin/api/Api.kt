package api

import area_element.AreaElementJson
import area_element.toAreaElementsJson
import element.Element
import element.toElements
import element_comment.ElementCommentJson
import element_comment.toElementCommentsJson
import event.Event
import json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.brotli.BrotliInterceptor
import okhttp3.coroutines.executeAsync
import reports.ReportJson
import reports.toReportsJson
import user.UserJson
import user.toUsersJson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val BASE_URL = "https://api.btcmap.org".toHttpUrl()

interface Api {
    suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<Element>

    suspend fun getElementComments(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<ElementCommentJson>

    suspend fun getEvents(): List<Event>

    suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson>

    suspend fun getUsers(updatedSince: ZonedDateTime?, limit: Long): List<UserJson>

    suspend fun getAreaElements(updatedSince: ZonedDateTime?, limit: Long): List<AreaElementJson>
}

class ApiImpl(
    private val baseUrl: HttpUrl = BASE_URL,
    private val httpClient: OkHttpClient = apiHttpClient(),
) : Api {
    override suspend fun getEvents(): List<Event> {
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
                    Event(
                        id = it.getLong("id"),
                        lat = it.getDouble("lat"),
                        lon = it.getDouble("lon"),
                        name = it.getString("name"),
                        website = it.getString("website").toHttpUrl(),
                        starts_at = ZonedDateTime.parse(it.getString("starts_at")),
                        ends_at = if (it.isNull("ends_at")) null else ZonedDateTime.parse(
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

    override suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<Element> {
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
            res.body.byteStream().use { it.toElements() }
        }
    }

    override suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("reports")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toReportsJson() }
        }
    }

    override suspend fun getUsers(updatedSince: ZonedDateTime?, limit: Long): List<UserJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("users")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toUsersJson() }
        }
    }

    override suspend fun getAreaElements(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<AreaElementJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("area-elements")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toAreaElementsJson() }
        }
    }
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