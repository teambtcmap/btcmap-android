package org.btcmap.api

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.InputStream

class ApiException(val code: Int, message: String) : Exception(message)

class Api(
    internal val httpClient: OkHttpClient,
    internal val url: HttpUrl,
) {
    internal suspend fun <T> call(request: Request, parse: (InputStream) -> T): T {
        return httpClient.newCall(request).executeAsync().use { res ->
            if (!res.isSuccessful) {
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
