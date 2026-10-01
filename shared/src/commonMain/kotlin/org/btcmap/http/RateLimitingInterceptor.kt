package org.btcmap.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.random.Random

object RateLimitingInterceptor : Interceptor {
    private const val MAX_RETRY_ATTEMPTS = 10
    private const val MAX_RETRY_DELAY_MS = 60_000L

    private val idempotentMethods = setOf("GET", "HEAD", "OPTIONS", "TRACE", "PUT", "DELETE")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var res = chain.proceed(request)

        if (request.method !in idempotentMethods) {
            return res
        }

        var retryAttempts = 0

        while (res.code == 429 && retryAttempts < MAX_RETRY_ATTEMPTS) {
            if (chain.call().isCanceled()) {
                res.close()
                throw IOException("Canceled while waiting to retry ${request.url}")
            }

            val retryAfter = res.retryAfterMillis()

            // A server-requested cool-down longer than we are willing to block
            // for is a signal to stop instead of hammering the endpoint.
            if (retryAfter != null && retryAfter > MAX_RETRY_DELAY_MS) {
                return res
            }

            val delay = (retryAfter ?: fallbackDelay(retryAttempts)).coerceAtLeast(0)

            res.close()

            try {
                if (delay > 0) {
                    Thread.sleep(delay)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting to retry ${request.url}", e)
            }

            // The caller may have cancelled while we were waiting; re-issuing
            // a request nobody awaits would only waste a connection.
            if (chain.call().isCanceled()) {
                throw IOException("Canceled while waiting to retry ${request.url}")
            }

            res = chain.proceed(request)
            retryAttempts++
        }

        return res
    }

    private fun fallbackDelay(retryAttempts: Int): Long {
        return (retryAttempts * 1000L + Random.nextLong(1000)).coerceAtMost(MAX_RETRY_DELAY_MS)
    }
}

internal fun Response.retryAfterMillis(): Long? {
    val value = header("Retry-After") ?: return null

    value.toLongOrNull()?.let { return it * 1000 }

    return try {
        val date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
        (date.toEpochSecond() - ZonedDateTime.now().toEpochSecond()).coerceAtLeast(0) * 1000
    } catch (e: DateTimeParseException) {
        null
    }
}
