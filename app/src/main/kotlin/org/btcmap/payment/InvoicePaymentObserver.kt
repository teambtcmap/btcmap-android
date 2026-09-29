package org.btcmap.payment

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    onState: (InvoicePaymentState<TQuote>) -> Unit,
    onPaid: () -> Unit,
) {
    val poller = InvoicePaymentPoller { invoiceId -> api().awaitPaidInvoice(invoiceId) }

    viewLifecycleOwner.lifecycleScope.launch {
        // The poller is created once per view and declared outside
        // repeatOnLifecycle so it survives the background and foreground
        // restarts of the block below: a paid invoice must be reported once per
        // view, not once per resume. Recreating the view resets it, so a screen
        // that was paid while away still closes.
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            launch {
                viewModel.state.collect { onState(it) }
            }

            launch {
                poller.awaitPayment(
                    ids = viewModel.state.map { it.invoice?.id },
                    onPaid = onPaid,
                    onFailure = viewModel::reportPaymentFailure,
                )
            }

            launch {
                viewModel.events.collect { event -> showError(event) }
            }
        }
    }
}

private fun Fragment.showError(event: PaymentEvent) {
    val error = when (event) {
        is PaymentEvent.QuoteFailed -> {
            // Without a quote there is nothing this screen can show.
            parentFragmentManager.popBackStack()
            event.error
        }

        is PaymentEvent.OrderFailed -> event.error

        is PaymentEvent.PaymentFailed -> event.error
    }

    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.error)
        .setMessage(error.userFacingMessage(getString(R.string.error)))
        .setPositiveButton(R.string.close, null)
        .show()
}
