package org.btcmap.boost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.btcmap.api.PlaceBoostQuoteResponse
import org.btcmap.payment.InvoicePaymentViewModel

internal class BoostViewModel(
    quoteLoader: suspend () -> PlaceBoostQuoteResponse,
) : InvoicePaymentViewModel<PlaceBoostQuoteResponse>(quoteLoader) {

    class Factory(
        private val quoteLoader: suspend () -> PlaceBoostQuoteResponse,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BoostViewModel(quoteLoader) as T
    }
}
