package org.btcmap.http

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.parse
import kotlin.random.Random

private const val MAX_RETRY_ATTEMPTS = 10
private const val MAX_RETRY_DELAY_MS = 60_000L

/**
 * Methods safe to re-issue. A POST that was rate-limited must not be retried
 * automatically: the server may have already applied it.
 */
private val idempotentMethods = setOf(
    HttpMethod.Get,
    HttpMethod.Head,
    HttpMethod.Options,
    HttpMethod.Put,
    HttpMethod.Delete,
)

/**
 * Sends [method] to [url] and, for an idempotent request the server rate-limits
 * with a 429, waits out its `Retry-After` (or a small backoff) and tries again.
 *
 * A server-requested cool-down longer than [MAX_RETRY_DELAY_MS] is a signal to
 * stop instead of hammering the endpoint, and the wait is a coroutine `delay`,
 * so cancelling the caller cancels the retry cleanly.
 */
internal suspend fun HttpClient.executeIdempotent(
    method: HttpMethod,
    url: Url,
    configure: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    var attempt = 0
    while (true) {
        val response = request(url) {
            this.method = method
            configure()
        }

        if (response.status != HttpStatusCode.TooManyRequests ||
            method !in idempotentMethods ||
            attempt >= MAX_RETRY_ATTEMPTS
        ) {
            return response
        }

        val retryAfter = response.headers.retryAfterMillis()
        if (retryAfter != null && retryAfter > MAX_RETRY_DELAY_MS) {
            return response
        }

        val delayMillis = (retryAfter ?: fallbackDelay(attempt)).coerceAtLeast(0)

        // Drain the rejected body so its connection is released before the wait.
        runCatching { response.bodyAsText() }

        if (delayMillis > 0) {
            delay(delayMillis)
        }

        attempt++
    }
}

private fun fallbackDelay(retryAttempts: Int): Long {
    return (retryAttempts * 1000L + Random.nextLong(1000)).coerceAtMost(MAX_RETRY_DELAY_MS)
}

internal fun Headers.retryAfterMillis(): Long? {
    val value = this[HttpHeaders.RetryAfter] ?: return null

    value.toLongOrNull()?.let { return it * 1000 }

    return try {
        val date = Instant.parse(value, DateTimeComponents.Formats.RFC_1123)
        (date - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
    } catch (e: RuntimeException) {
        null
    }
}
