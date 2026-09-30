package org.btcmap.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** A Lightning invoice the user can pay. */
internal data class PaymentInvoice(
    val id: String,
    val bolt11: String,
)

/** One-shot failures to show to the user exactly once. */
internal sealed interface PaymentEvent {
    data class QuoteFailed(val error: Throwable) : PaymentEvent

    data class OrderFailed(val error: Throwable) : PaymentEvent

    /**
     * The invoice could not be polled to completion, for example because the
     * server rejected it permanently. Raised by the view layer, which owns the
     * polling.
     */
    data class PaymentFailed(val error: Throwable) : PaymentEvent
}

/**
 * State shared by every invoice payment screen.
 *
 * [actionsEnabled] gates the controls that start an order, [inputEnabled] gates
 * the input the order is built from. The invoice lives here so a rotation does
 * not lose a payment the user is about to make.
 */
internal data class InvoicePaymentState<TQuote : Any>(
    val quote: TQuote? = null,
    val invoice: PaymentInvoice? = null,
    val loadingQuote: Boolean = true,
    val ordering: Boolean = false,
) {
    val actionsEnabled: Boolean
        get() = quote != null && !loadingQuote && !ordering && invoice == null

    val inputEnabled: Boolean
        get() = !ordering && invoice == null
}

/**
 * Drives the quote, order and invoice part of a payment screen, shared by the
 * merchant boost and comment flows.
 *
 * A feature supplies how to load its quote and how to place its order; this
 * class guarantees the quote is loaded once and that a second order is never
 * placed once an invoice exists, so a repeated tap cannot trigger a second
 * charge. The pending invoice is kept in [savedStateHandle], so an order the
 * user is in the middle of paying survives the process being killed.
 */
internal class InvoicePaymentViewModel<TQuote : Any>(
    private val quoteLoader: suspend () -> TQuote,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        InvoicePaymentState<TQuote>(invoice = savedStateHandle.restoredInvoice()),
    )
    val state: StateFlow<InvoicePaymentState<TQuote>> = _state.asStateFlow()

    private val _events = Channel<PaymentEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEvent> = _events.receiveAsFlow()

    private var quoteRequested = false

    // An invoice restored after process death has already been ordered, so it
    // must not be ordered again.
    private var orderRequested = _state.value.invoice != null

    fun loadQuote() {
        if (quoteRequested) return
        quoteRequested = true

        // Set before the launch so a retry after a failure shows the loading
        // state again instead of briefly looking like it did nothing.
        _state.update { it.copy(loadingQuote = true) }

        viewModelScope.launch {
            try {
                val quote = quoteLoader()
                _state.update { it.copy(quote = quote, loadingQuote = false) }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                quoteRequested = false
                _state.update { it.copy(loadingQuote = false) }
                _events.trySend(PaymentEvent.QuoteFailed(t))
            }
        }
    }

    fun order(place: suspend () -> PaymentInvoice) {
        // No quote means the price has not been shown yet; the screen gates the
        // controls on the same condition, so this only closes the window before
        // the first render.
        if (orderRequested || _state.value.invoice != null || _state.value.quote == null) return
        orderRequested = true

        viewModelScope.launch {
            _state.update { it.copy(ordering = true) }
            try {
                val invoice = place()
                savedStateHandle.storeInvoice(invoice)
                _state.update { it.copy(invoice = invoice) }
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                orderRequested = false
                _events.trySend(PaymentEvent.OrderFailed(t))
            } finally {
                _state.update { it.copy(ordering = false) }
            }
        }
    }

    /**
     * Forgets the current invoice so the user can adjust the input and order
     * again, for example when the invoice cannot be paid or polling gave up.
     *
     * There is no endpoint to cancel an invoice, so it is only dropped locally
     * and stays payable until it expires: paying the discarded invoice after
     * ordering a new one would charge the user twice. The screen confirms
     * before calling this.
     */
    fun startOver() {
        if (_state.value.invoice == null) return
        orderRequested = false
        savedStateHandle.clearInvoice()
        _state.update { it.copy(invoice = null) }
    }

    /**
     * Reports a failure raised while polling an invoice for payment. The
     * polling lives in the view layer, so it cannot use the [order] and
     * [loadQuote] error handling and hands the failure back here instead, to
     * keep a single error channel for the screen.
     */
    fun reportPaymentFailure(error: Throwable) {
        _events.trySend(PaymentEvent.PaymentFailed(error))
    }
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
