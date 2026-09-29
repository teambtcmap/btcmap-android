package org.btcmap.payment

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Returns this fragment's [InvoicePaymentViewModel], creating it with
 * [quoteLoader] the first time it is requested.
 *
 * The view model type is erased for the lookup, so the cast back to the quote
 * type is safe as long as the loader and the requested type agree, which the
 * property declaration enforces at the call site. The view model gets the
 * fragment's [androidx.lifecycle.SavedStateHandle], so a pending invoice
 * survives the process being killed.
 */
@Suppress("UNCHECKED_CAST")
internal fun <TQuote : Any> Fragment.invoicePaymentViewModel(
    quoteLoader: suspend () -> TQuote,
): InvoicePaymentViewModel<TQuote> {
    val factory = viewModelFactory {
        initializer {
            InvoicePaymentViewModel(quoteLoader, createSavedStateHandle())
        }
    }

    return ViewModelProvider(this, factory)[InvoicePaymentViewModel::class.java]
        as InvoicePaymentViewModel<TQuote>
}
