package org.btcmap.api

import kotlinx.coroutines.delay
import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.util.toJsonObject

private const val PAYMENT_POLL_INTERVAL_MS = 500L

data class Invoice(
    val id: String,
    val status: String,
)

val Invoice.paid: Boolean
    get() = status == "paid"

suspend fun Api.getInvoice(id: String): Invoice {
    val url = buildUrl("v4", "invoices", id)

    return call(Request.Builder().withoutAuth().url(url).build()) { stream ->
        val body = stream.toJsonObject()

        Invoice(
            id = body.string("id"),
            status = body.string("status"),
        )
    }
}

/**
 * Polls [id] until the invoice is paid, retrying transient failures. Call this
 * from a lifecycle-bound coroutine so the loop stops when the screen is no
 * longer visible; cancellation propagates instead of being retried.
 */
suspend fun Api.awaitPaidInvoice(
    id: String,
    pollIntervalMillis: Long = PAYMENT_POLL_INTERVAL_MS,
): Invoice {
    while (true) {
        val invoice = try {
            getInvoice(id)
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            delay(pollIntervalMillis)
            continue
        }

        if (invoice.paid) {
            return invoice
        }

        delay(pollIntervalMillis)
    }
}
