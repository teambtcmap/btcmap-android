package org.btcmap.boost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.PlaceBoostQuoteResponse
import org.btcmap.api.boostPlace
import org.btcmap.api.getPlaceBoostQuote
import org.btcmap.databinding.BoostFragmentBinding
import org.btcmap.payment.InvoicePaymentController
import org.btcmap.payment.InvoicePaymentState
import org.btcmap.payment.InvoicePaymentViewModel
import org.btcmap.payment.PaymentInvoice
import org.btcmap.payment.invoicePaymentViewModel
import org.btcmap.payment.observeInvoicePayment
import org.btcmap.ui.BoostFormUiState
import org.btcmap.ui.BoostOption
import java.text.NumberFormat

class BoostFragment : Fragment() {

    private data class Args(val placeId: Long, val placeName: String?)

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            placeName = requireArguments().getString("place_name"),
        )
    }

    private var _binding: BoostFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoicePaymentViewModel<PlaceBoostQuoteResponse> by lazy {
        invoicePaymentViewModel { api().getPlaceBoostQuote() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BoostFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.topAppBar.title = args.placeName ?: getString(R.string.boost_merchant)

        val payment = InvoicePaymentController(
            fragment = this,
            view = binding.invoicePayment,
            paymentRequestLabel = getString(R.string.btc_map_boost_payment_request),
            onStartOver = viewModel::startOver,
        )

        observeInvoicePayment(
            viewModel = viewModel,
            onState = { render(it, payment) },
            onPaid = {
                (activity as? Activity)?.showMessage(
                    getString(R.string.your_boost_is_active),
                )
                parentFragmentManager.popBackStack()
            },
        )

        binding.boostForm.onContinue = { key ->
            BoostDuration.entries.firstOrNull { it.name == key }?.let { duration ->
                viewModel.order {
                    val response = api().boostPlace(placeId = args.placeId, days = duration.days)
                    PaymentInvoice(id = response.invoiceId, bolt11 = response.invoice)
                }
            }
        }

        viewModel.loadQuote()
    }

    private fun render(
        state: InvoicePaymentState<PlaceBoostQuoteResponse>,
        payment: InvoicePaymentController,
    ) {
        val quote = state.quote
        val options = BoostDuration.entries.map { duration ->
            val label = getString(duration.labelRes)
            val price = when {
                quote != null -> getString(
                    R.string.d_sat,
                    NumberFormat.getNumberInstance().format(duration.priceSat(quote)),
                )

                state.loadingQuote -> getString(R.string.loading_quote)
                else -> null
            }
            BoostOption(
                key = duration.name,
                label = price?.let { getString(R.string.duration_with_price, label, it) } ?: label,
            )
        }

        // The options and continue button are locked until the quote is loaded,
        // while an order is in flight, and once an invoice exists, so a second
        // boost cannot be ordered (and charged) by tapping continue again.
        _binding?.boostForm?.state = BoostFormUiState(
            description = getString(R.string.boost_description),
            durationTitle = getString(R.string.boost_duration),
            options = options,
            continueLabel = getString(R.string.btn_continue),
            selectedKey = BoostDuration.THREE_MONTHS.name,
            optionsEnabled = state.actionsEnabled,
            actionsEnabled = state.actionsEnabled,
            // The invoice block replaces the order controls, so the continue
            // button that started the order is hidden once one exists.
            showContinue = state.invoice == null,
        )

        val invoice = state.invoice
        if (invoice == null) {
            payment.hide()
        } else {
            payment.show(invoice)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
