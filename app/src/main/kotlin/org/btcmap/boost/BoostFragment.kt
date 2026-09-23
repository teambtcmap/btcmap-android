package org.btcmap.boost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import java.text.NumberFormat

class BoostFragment : Fragment() {

    private data class Args(val placeId: Long)

    private val args by lazy { Args(requireArguments().getLong("place_id")) }

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

        val payment = InvoicePaymentController(
            fragment = this,
            qr = binding.qr,
            payButton = binding.payInvoice,
            copyButton = binding.copyInvoice,
            paymentRequestLabel = getString(R.string.btc_map_boost_payment_request),
        )

        observeInvoicePayment(
            viewModel = viewModel,
            onState = { render(it, payment) },
            onPaid = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.your_boost_is_active),
                    Toast.LENGTH_LONG,
                ).show()
                parentFragmentManager.popBackStack()
            },
        )

        binding.btnContinue.setOnClickListener {
            val duration = selectedDuration() ?: return@setOnClickListener
            viewModel.order {
                val response = api().boostPlace(placeId = args.placeId, days = duration.days)
                PaymentInvoice(id = response.invoiceId, bolt11 = response.invoice)
            }
        }

        viewModel.loadQuote()
    }

    private fun render(
        state: InvoicePaymentState<PlaceBoostQuoteResponse>,
        payment: InvoicePaymentController,
    ) {
        state.quote?.let { quote ->
            BoostDuration.entries.forEach { duration ->
                setDurationPrice(button(duration), duration, duration.priceSat(quote))
            }
        }

        // The options and continue button are locked until the quote is loaded,
        // while an order is in flight, and once an invoice exists, so a second
        // boost cannot be ordered (and charged) by tapping continue again.
        val enabled = state.actionsEnabled
        BoostDuration.entries.forEach { button(it).isEnabled = enabled }
        binding.btnContinue.isEnabled = enabled

        val invoice = state.invoice
        if (invoice == null) {
            payment.hide()
        } else {
            payment.show(invoice)
        }
    }

    private fun setDurationPrice(button: RadioButton, duration: BoostDuration, priceSat: Long) {
        val price = getString(R.string.d_sat, NumberFormat.getNumberInstance().format(priceSat))
        button.text = getString(R.string.duration_with_price, getString(duration.labelRes), price)
    }

    private fun button(duration: BoostDuration): RadioButton = when (duration) {
        BoostDuration.ONE_MONTH -> binding.boost1m
        BoostDuration.THREE_MONTHS -> binding.boost3m
        BoostDuration.TWELVE_MONTHS -> binding.boost12m
    }

    private fun selectedDuration(): BoostDuration? =
        BoostDuration.fromButtonId(binding.durationOptions.checkedRadioButtonId)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
