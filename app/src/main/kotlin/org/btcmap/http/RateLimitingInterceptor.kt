package org.btcmap.http

import okhttp3.Interceptor
import okhttp3.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
            val delay = res.retryAfterMillis() ?: (retryAttempts * 1000L + Random.nextLong(1000))
            res.close()
            Thread.sleep(delay.coerceIn(0, MAX_RETRY_DELAY_MS))
            res = chain.proceed(request)
            retryAttempts++
        }

        return res
    }
}

internal fun Response.retryAfterMillis(): Long? {
    val value = header("Retry-After") ?: return null

    value.toLongOrNull()?.let { return it * 1000 }

    return runCatching {
        val date = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
        (date.toEpochSecond() - ZonedDateTime.now().toEpochSecond()).coerceAtLeast(0) * 1000
    }.getOrNull()
}
