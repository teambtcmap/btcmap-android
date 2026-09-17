package org.btcmap.payment

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
 * charge.
 */
internal open class InvoicePaymentViewModel<TQuote : Any>(
    private val quoteLoader: suspend () -> TQuote,
) : ViewModel() {

    private val _state = MutableStateFlow(InvoicePaymentState<TQuote>())
    val state: StateFlow<InvoicePaymentState<TQuote>> = _state.asStateFlow()

    private val _events = Channel<PaymentEvent>(Channel.BUFFERED)
    val events: Flow<PaymentEvent> = _events.receiveAsFlow()

    private var quoteRequested = false
    private var orderRequested = false

    fun loadQuote() {
        if (quoteRequested) return
        quoteRequested = true

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
        if (orderRequested || _state.value.invoice != null) return
        orderRequested = true

        viewModelScope.launch {
            _state.update { it.copy(ordering = true) }
            try {
                val invoice = place()
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
}
