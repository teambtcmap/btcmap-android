package org.btcmap.payment

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

/**
 * Returns this fragment's [InvoicePaymentViewModel], creating it with
 * [quoteLoader] the first time it is requested.
 *
 * The view model type is erased for the lookup, so the cast back to the quote
 * type is safe as long as the loader and the requested type agree, which the
 * property declaration enforces at the call site.
 */
@Suppress("UNCHECKED_CAST")
internal fun <TQuote : Any> Fragment.invoicePaymentViewModel(
    quoteLoader: suspend () -> TQuote,
): InvoicePaymentViewModel<TQuote> {
    return ViewModelProvider(
        this,
        InvoicePaymentViewModel.Factory(quoteLoader),
    )[InvoicePaymentViewModel::class.java] as InvoicePaymentViewModel<TQuote>
}
