package org.btcmap.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.InputStream

class ApiException(val code: Int, message: String) : Exception(message)

class ApiParseException(message: String) : Exception(message)

class Api(
    internal val httpClient: OkHttpClient,
    private val baseUrl: () -> HttpUrl,
    private val onUnauthorized: suspend () -> Unit = {},
) {
    internal val url: HttpUrl
        get() = baseUrl()

    internal fun buildUrl(vararg segments: String): HttpUrl {
        return url.newBuilder().apply {
            segments.forEach { addPathSegment(it) }
        }.build()
    }

    internal fun jsonBody(body: JsonObject): RequestBody {
        return body.toString().toRequestBody("application/json".toMediaType())
    }

    internal suspend fun <T> call(
        request: Request,
        clearSessionOnUnauthorized: Boolean = true,
        parse: (InputStream) -> T,
    ): T {
        return httpClient.newCall(request).executeAsync().use { res ->
            if (!res.isSuccessful) {
                if (res.code == 401 && clearSessionOnUnauthorized) {
                    onUnauthorized()
                }

                throw res.toApiException()
            }

            withContext(Dispatchers.IO) {
                res.body.byteStream().use(parse)
            }
        }
    }

    private fun Response.toApiException(): ApiException {
        val body = runCatching { body.string() }.getOrDefault("")

        val message = runCatching {
            JsonParser.parseString(body).asJsonObject.nonBlankStringOrNull("message")
        }.getOrNull()

        return ApiException(code, message ?: "HTTP $code: ${body.ifBlank { "unexpected response" }}")
    }
}
