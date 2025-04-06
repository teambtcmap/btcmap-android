package api

import area.AreaJson
import area.toAreasJson
import area_element.AreaElementJson
import area_element.toAreaElementsJson
import element.Element
import element.toElements
import element_comment.ElementCommentJson
import element_comment.toElementCommentsJson
import event.EventJson
import event.toEventsJson
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
    suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson>

    suspend fun getElementComments(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<ElementCommentJson>

    suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<Element>

    suspend fun getEvents(updatedSince: ZonedDateTime?, limit: Long): List<EventJson>

    suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson>

    suspend fun getUsers(updatedSince: ZonedDateTime?, limit: Long): List<UserJson>

    suspend fun getAreaElements(updatedSince: ZonedDateTime?, limit: Long): List<AreaElementJson>
}

class ApiImpl(
    private val baseUrl: HttpUrl = BASE_URL,
    private val httpClient: OkHttpClient = apiHttpClient(),
) : Api {
    override suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("areas")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toAreasJson() }
        }
    }

    override suspend fun getElementComments(
        updatedSince: ZonedDateTime?,
        limit: Long
    ): List<ElementCommentJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("element-comments")
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

    override suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<Element> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v4")
            addPathSegment("elements")

            addQueryParameter("f", "lat")
            addQueryParameter("f", "lon")
            addQueryParameter("f", "icon")
            addQueryParameter("f", "name")
            addQueryParameter("f", "updated_at")
            addQueryParameter("f", "deleted_at")

            addQueryParameter("f", "required_app_url")
            addQueryParameter("f", "boosted_until")
            addQueryParameter("f", "verified_at")
            addQueryParameter("f", "address")
            addQueryParameter("f", "opening_hours")
            addQueryParameter("f", "website")
            addQueryParameter("f", "phone")
            addQueryParameter("f", "email")
            addQueryParameter("f", "twitter")
            addQueryParameter("f", "facebook")
            addQueryParameter("f", "instagram")
            addQueryParameter("f", "line")

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

    override suspend fun getEvents(updatedSince: ZonedDateTime?, limit: Long): List<EventJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("events")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", "$limit")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toEventsJson() }
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