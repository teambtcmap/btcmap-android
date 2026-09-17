package org.btcmap.api

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
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
import java.io.IOException
import java.io.InputStream

class ApiException(
    val code: Int,
    message: String,
    val errorCode: String? = null,
) : Exception(message)

class ApiParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ApiTransportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class Api(
    internal val httpClient: OkHttpClient,
    private val baseUrl: () -> HttpUrl,
    private val onUnauthorized: suspend (requestToken: String?) -> Unit = {},
) {
    internal val url: HttpUrl
        get() = baseUrl()

    internal fun buildUrl(
        vararg segments: String,
        configure: HttpUrl.Builder.() -> Unit = {},
    ): HttpUrl {
        return url.newBuilder().apply {
            segments.forEach { addPathSegment(it) }
            configure()
        }.build()
    }

    internal fun jsonBody(body: JsonElement): RequestBody {
        return body.toString().toRequestBody("application/json".toMediaType())
    }

    internal suspend fun <T> call(
        request: Request,
        clearSessionOnUnauthorized: Boolean = true,
        parse: (InputStream) -> T,
    ): T {
        val response = try {
            httpClient.newCall(request).executeAsync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ApiTransportException("Failed to reach ${request.url}", e)
        }

        return response.use { res ->
            withContext(Dispatchers.IO) {
                if (!res.isSuccessful) {
                    val exception = res.toApiException()

                    // A 401 only invalidates the session when the request actually
                    // carried the stored token. A request sent without one (for
                    // example while signed out) must not clear a still-valid session.
                    val authorization = res.request.header("Authorization")
                    if (res.code == 401 && clearSessionOnUnauthorized && authorization != null) {
                        try {
                            onUnauthorized(authorization.toBearerToken())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            exception.addSuppressed(e)
                        }
                    }

                    throw exception
                }

                res.body.byteStream().use { stream ->
                    try {
                        parse(stream)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApiParseException) {
                        throw e
                    } catch (e: IOException) {
                        throw ApiTransportException("Failed to read response from ${request.url}", e)
                    } catch (e: RuntimeException) {
                        throw ApiParseException("Failed to parse response from ${request.url}", e)
                    }
                }
            }
        }
    }

    private fun String.toBearerToken(): String? =
        removePrefix("Bearer ").takeIf { it.isNotBlank() }

    private fun Response.toApiException(): ApiException {
        val body = try {
            body.string()
        } catch (e: IOException) {
            ""
        }

        val errorBody = try {
            JsonParser.parseString(body).asJsonObject
        } catch (e: RuntimeException) {
            null
        }

        val message = errorBody?.nonBlankStringOrNull("message")
        val errorCode = errorBody?.nonBlankStringOrNull("code")

        return ApiException(
            code = code,
            message = message ?: "HTTP $code: ${body.ifBlank { "unexpected response" }}",
            errorCode = errorCode,
        )
    }
}
