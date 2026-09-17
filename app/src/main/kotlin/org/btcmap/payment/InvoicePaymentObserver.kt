package org.btcmap.payment

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.awaitPaidInvoice
import org.btcmap.util.userFacingMessage

/**
 * Renders [viewModel]'s state, polls the current invoice until it is paid, and
 * reports failures, all while the fragment is resumed.
 *
 * Tying the work to the view lifecycle means polling stops in the background
 * and is restarted after a rotation, picking the invoice back up from the
 * retained state instead of starting over.
 */
internal fun <TQuote : Any> Fragment.observeInvoicePayment(
    viewModel: InvoicePaymentViewModel<TQuote>,
    logTag: String,
    onState: (InvoicePaymentState<TQuote>) -> Unit,
    onPaid: () -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                viewModel.state.collect { onState(it) }
            }

            launch {
                viewModel.state
                    .map { it.invoice?.id }
                    .distinctUntilChanged()
                    .collect { invoiceId ->
                        if (invoiceId != null) {
                            api().awaitPaidInvoice(invoiceId)
                            onPaid()
                        }
                    }
            }

            launch {
                viewModel.events.collect { event -> showError(logTag, event) }
            }
        }
    }
}

private fun Fragment.showError(logTag: String, event: PaymentEvent) {
    val error = when (event) {
        is PaymentEvent.QuoteFailed -> {
            // Without a quote there is nothing this screen can show.
            parentFragmentManager.popBackStack()
            event.error
        }

        is PaymentEvent.OrderFailed -> event.error
    }

    Log.e(logTag, "Payment failed", error)

    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.error)
        .setMessage(error.userFacingMessage(getString(R.string.error)))
        .setPositiveButton(R.string.close, null)
        .show()
}
