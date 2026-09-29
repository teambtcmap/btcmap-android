package org.btcmap.payment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.btcmap.util.rethrowIfCancellation

/**
 * Watches the invoice id a payment screen currently shows and waits until it is
 * paid.
 *
 * Only the latest id is polled: when the id changes (the user starts over and
 * orders again) the previous poll is cancelled instead of outliving the state
 * that started it. [awaitPaidInvoice] returns once the invoice is paid and
 * throws on a permanent failure; the first success is [onPaid].
 *
 * The instance keeps the already-paid result, so the same payment is not
 * reported again when the view resumes. A screen creates one instance per view,
 * so a recreated view starts fresh.
 */
internal class InvoicePaymentPoller(
    private val awaitPaidInvoice: suspend (String) -> Unit,
) {

    private var paid = false

    suspend fun awaitPayment(
        ids: Flow<String?>,
        onPaid: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        ids.distinctUntilChanged().collectLatest { invoiceId ->
            if (invoiceId == null || paid) return@collectLatest
            try {
                awaitPaidInvoice(invoiceId)
                paid = true
                onPaid()
            } catch (t: Throwable) {
                // A permanent failure (e.g. the server no longer knows the
                // invoice) must surface, not crash the lifecycle coroutine.
                t.rethrowIfCancellation()
                onFailure(t)
            }
        }
    }
}
