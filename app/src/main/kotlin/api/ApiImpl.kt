package api

import area.AreaJson
import area.toAreasJson
import element.ElementJson
import element.toElementsJson
import event.EventJson
import event.toEventsJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import reports.ReportJson
import reports.toReportsJson
import user.UserJson
import user.toUsersJson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ApiImpl(
    private val baseUrl: HttpUrl,
    private val httpClient: OkHttpClient,
) : Api {

    override suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v2")
            addPathSegment("areas")

            if (updatedSince != null) {
                addQueryParameter("updated_since", updatedSince.apiFormat())
            }

            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.executeAsync()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    responseBody.toAreasJson()
                }
            }
        }
    }

    override suspend fun getElements(updatedSince: ZonedDateTime?, limit: Long): List<ElementJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v3")
            addPathSegment("elements")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.executeAsync()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    responseBody.toElementsJson()
                }
            }
        }
    }

    override suspend fun getEvents(updatedSince: ZonedDateTime?, limit: Long): List<EventJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v2")
            addPathSegment("events")

            if (updatedSince != null) {
                addQueryParameter("updated_since", updatedSince.toString())
            }

            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.executeAsync()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    responseBody.toEventsJson()
                }
            }
        }
    }

    override suspend fun getReports(updatedSince: ZonedDateTime?, limit: Long): List<ReportJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v2")
            addPathSegment("reports")
            addQueryParameter("updated_since", updatedSince.apiFormat())
            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.executeAsync()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    responseBody.toReportsJson()
                }
            }
        }
    }

    override suspend fun getUsers(updatedSince: ZonedDateTime?, limit: Long): List<UserJson> {
        val url = baseUrl.newBuilder().apply {
            addPathSegment("v2")
            addPathSegment("users")

            if (updatedSince != null) {
                addQueryParameter("updated_since", updatedSince.apiFormat())
            }

            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.executeAsync()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    responseBody.toUsersJson()
                }
            }
        }
    }
}

private fun ZonedDateTime?.apiFormat(): String {
    return this?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: "2020-01-01T00:00:00Z"
}