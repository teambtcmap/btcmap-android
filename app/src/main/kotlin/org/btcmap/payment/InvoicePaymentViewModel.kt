package org.btcmap.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android host for the shared [InvoicePaymentFlow].
 *
 * The state machine itself lives in `:shared`; this only gives it the
 * [viewModelScope] to run in and the fragment's [SavedStateHandle], so the
 * offline and desktop hosts share the same quote, order and invoice behaviour
 * while a pending invoice still survives the process being killed on Android.
 */
internal class InvoicePaymentViewModel<TQuote : Any>(
    quoteLoader: suspend () -> TQuote,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val flow = InvoicePaymentFlow(
        quoteLoader = quoteLoader,
        scope = viewModelScope,
        restoredInvoice = savedStateHandle.restoredInvoice(),
        persistInvoice = { savedStateHandle.storeInvoice(it) },
        clearPersistedInvoice = { savedStateHandle.clearInvoice() },
    )

    val state: StateFlow<InvoicePaymentState<TQuote>> = flow.state

    val events: Flow<PaymentEvent> = flow.events

    fun loadQuote() = flow.loadQuote()

    fun order(place: suspend () -> PaymentInvoice) = flow.order(place)

    fun startOver() = flow.startOver()

    fun reportPaymentFailure(error: Throwable) = flow.reportPaymentFailure(error)
}

private const val INVOICE_ID_KEY = "org.btcmap.payment.invoiceId"
private const val INVOICE_BOLT11_KEY = "org.btcmap.payment.invoiceBolt11"

/** The invoice a previous process saved, or null when there is none. */
private fun SavedStateHandle.restoredInvoice(): PaymentInvoice? {
    val id = get<String>(INVOICE_ID_KEY) ?: return null
    val bolt11 = get<String>(INVOICE_BOLT11_KEY) ?: return null
    return PaymentInvoice(id = id, bolt11 = bolt11)
}

private fun SavedStateHandle.storeInvoice(invoice: PaymentInvoice) {
    this[INVOICE_ID_KEY] = invoice.id
    this[INVOICE_BOLT11_KEY] = invoice.bolt11
}

private fun SavedStateHandle.clearInvoice() {
    remove<String>(INVOICE_ID_KEY)
    remove<String>(INVOICE_BOLT11_KEY)
}
