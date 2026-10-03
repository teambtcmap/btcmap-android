package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.coroutines.delay
import org.btcmap.util.toJsonObject

private const val PAYMENT_POLL_INTERVAL_MS = 500L
private const val PAYMENT_POLL_MAX_INTERVAL_MS = 5_000L

data class Invoice(
    val id: String,
    val status: String,
)

val Invoice.paid: Boolean
    get() = status == "paid"

suspend fun Api.getInvoice(id: String): Invoice {
    val url = buildUrl("v4", "invoices", id)

    return call(HttpMethod.Get, url, withoutAuth = true) { body ->
        val parsed = body.toJsonObject()

        Invoice(
            id = parsed.string("id"),
            status = parsed.string("status"),
        )
    }
}

/**
 * Polls [id] until the invoice is paid.
 *
 * Permanent failures (4xx and parse errors) are rethrown so the caller can
 * surface them instead of polling forever. Everything else waits out a capped
 * exponential backoff: each attempt doubles the wait from
 * [initialPollIntervalMillis] up to [maxPollIntervalMillis], whether the invoice
 * is simply still unpaid or the last attempt failed transiently (transport and
 * 5xx). Polling an invoice the user may leave open for minutes therefore backs
 * off to a slow trickle instead of hammering the server every
 * [initialPollIntervalMillis]. Call this from a lifecycle-bound coroutine so the
 * loop stops when the screen is no longer visible; cancellation propagates
 * instead of being retried.
 */
suspend fun Api.awaitPaidInvoice(
    id: String,
    initialPollIntervalMillis: Long = PAYMENT_POLL_INTERVAL_MS,
    maxPollIntervalMillis: Long = PAYMENT_POLL_MAX_INTERVAL_MS,
): Invoice {
    var pollIntervalMillis = initialPollIntervalMillis
    while (true) {
        val invoice = try {
            getInvoice(id)
        } catch (e: ApiError) {
            if (!e.retryable) {
                throw e
            }
            delay(pollIntervalMillis)
            pollIntervalMillis = nextPollInterval(pollIntervalMillis, maxPollIntervalMillis)
            continue
        }

        if (invoice.paid) {
            return invoice
        }

        delay(pollIntervalMillis)
        pollIntervalMillis = nextPollInterval(pollIntervalMillis, maxPollIntervalMillis)
    }
}

/** Doubles [current], capped at [max]. */
internal fun nextPollInterval(current: Long, max: Long): Long =
    (current * 2).coerceAtMost(max)
