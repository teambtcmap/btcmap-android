package api

import area.AreaJson
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZonedDateTime

class ApiImpl(
    private val httpClient: OkHttpClient,
    private val json: Json,
) : Api {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getAreas(updatedSince: ZonedDateTime?, limit: Long): List<AreaJson> {
        val url = HttpUrl.Builder().apply {
            scheme("https")
            host("api.btcmap.org")
            addPathSegment("v2")
            addPathSegment("areas")

            if (updatedSince != null) {
                addQueryParameter("updated_since", updatedSince.toString())
            }

            addQueryParameter("limit", limit.toString())
        }.build()

        val request = httpClient.newCall(Request.Builder().url(url).build())
        val response = request.await()

        if (!response.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${response.code}")
        }

        return withContext(Dispatchers.IO) {
            response.body!!.byteStream().use { responseBody ->
                withContext(Dispatchers.IO) {
                    json.decodeFromStream(
                        stream = responseBody,
                        deserializer = ListSerializer(AreaJson.serializer()),
                    )
                }
            }
        }
    }
}