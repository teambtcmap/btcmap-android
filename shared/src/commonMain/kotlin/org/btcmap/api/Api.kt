package org.btcmap.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import okio.IOException
import org.btcmap.http.executeIdempotent
import org.btcmap.json.parseJson

/**
 * Base type for every failure raised by [Api]. Coroutine cancellation is not an
 * [ApiError] and always propagates untouched, so catching this type never
 * swallows it. Use [retryable] to decide whether re-issuing the request could
 * plausibly succeed.
 */
sealed class ApiError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Whether repeating the same request could eventually succeed. Transport
     * failures and server-side (5xx) responses are transient; client errors
     * (4xx) and parse failures will repeat until something else changes.
     */
    abstract val retryable: Boolean
}

/** The server answered with a non-2xx status. */
class ApiException(
    val code: Int,
    message: String,
    val errorCode: String? = null,
) : ApiError(message) {
    override val retryable: Boolean get() = code >= 500
}

/** The response could not be parsed into the expected shape. */
class ApiParseException(
    message: String,
    cause: Throwable? = null,
) : ApiError(message, cause) {
    override val retryable: Boolean get() = false
}

/** The request never produced a readable response (connection/read failure). */
class ApiTransportException(
    message: String,
    cause: Throwable? = null,
) : ApiError(message, cause) {
    override val retryable: Boolean get() = true
}

class Api(
    internal val httpClient: HttpClient,
    private val baseUrl: () -> Url,
    /**
     * The stored session token. It is attached per request by [call], which is
     * the only code that can tell whether a rejected request carried it, so a
     * 401 clears the session only when it did.
     */
    private val token: () -> String? = { null },
    private val onUnauthorized: suspend (requestToken: String?) -> Unit = {},
    /**
     * Identifies this app to the server as the `origin` of a place report or
     * submission. Injected because it embeds the app's version code, which only
     * the platform entry point knows.
     */
    val userAgent: String = "BTC Map",
) {
    internal val url: Url
        get() = baseUrl()

    internal fun buildUrl(
        vararg segments: String,
        configure: URLBuilder.() -> Unit = {},
    ): Url {
        return URLBuilder(url).apply {
            segments.forEach { appendPathSegments(it) }
            configure()
        }.build()
    }

    /**
     * Sends one request and parses its body.
     *
     * [authorization] is an explicit `Authorization` header value (used by
     * sign-in, which carries the password as a bearer token). Otherwise the
     * stored [token] is attached to every same-origin request, so the server can
     * prioritize requests from signed-in users. Account creation is the only
     * request that opts out with [withoutAuth]: it runs before a session exists.
     * [parse] receives the response body as text.
     */
    internal suspend fun <T> call(
        method: HttpMethod,
        url: Url,
        authorization: String? = null,
        withoutAuth: Boolean = false,
        body: JsonElement? = null,
        clearSessionOnUnauthorized: Boolean = true,
        parse: (String) -> T,
    ): T {
        val attachedToken = attachedToken(url, authorization, withoutAuth)
        val headerValue = authorization ?: attachedToken?.let { "Bearer $it" }

        val response = try {
            httpClient.executeIdempotent(method, url) {
                headerValue?.let { header(HttpHeaders.Authorization, it) }
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ApiTransportException("Failed to reach $url", e)
        }

        if (!response.status.isSuccess()) {
            val exception = response.toApiException()

            // A 401 only invalidates the session when the request actually
            // carried the stored token. A request sent without one (for example
            // while signed out) must not clear a still-valid session.
            if (response.status.value == 401 && clearSessionOnUnauthorized && attachedToken != null) {
                try {
                    onUnauthorized(attachedToken)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    exception.addSuppressed(e)
                }
            }

            throw exception
        }

        val text = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ApiTransportException("Failed to read response from $url", e)
        }

        return try {
            parse(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiParseException) {
            throw e
        } catch (e: RuntimeException) {
            throw ApiParseException("Failed to parse response from $url", e)
        }
    }

    /**
     * The token to attach to a request to [url], or null when the request opts
     * out ([withoutAuth], account creation), the token is blank, or the host is
     * not the configured API. A malformed stored API URL is treated like a
     * foreign host.
     */
    private fun attachedToken(url: Url, authorization: String?, withoutAuth: Boolean): String? {
        authorization?.removePrefix("Bearer ")?.takeIf { it.isNotBlank() }?.let { return it }
        if (withoutAuth) return null

        val base = runCatching { baseUrl() }.getOrNull() ?: return null
        if (url.protocol != base.protocol || url.host != base.host || url.port != base.port) return null

        return runCatching { token() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private suspend fun HttpResponse.toApiException(): ApiException {
        val code = status.value
        val body = try {
            bodyAsText()
        } catch (e: IOException) {
            ""
        }

        val errorBody = try {
            parseJson(body).jsonObject
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
