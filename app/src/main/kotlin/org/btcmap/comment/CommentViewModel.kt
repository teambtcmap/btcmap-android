package org.btcmap.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.btcmap.api.CommentQuoteResponse
import org.btcmap.payment.InvoicePaymentViewModel

internal class CommentViewModel(
    quoteLoader: suspend () -> CommentQuoteResponse,
) : InvoicePaymentViewModel<CommentQuoteResponse>(quoteLoader) {

    class Factory(
        private val quoteLoader: suspend () -> CommentQuoteResponse,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CommentViewModel(quoteLoader) as T
    }
}
