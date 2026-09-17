package org.btcmap.boost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.PlaceBoostQuoteResponse
import org.btcmap.api.boostPlace
import org.btcmap.api.getPlaceBoostQuote
import org.btcmap.databinding.BoostFragmentBinding
import org.btcmap.payment.InvoicePaymentController
import org.btcmap.payment.InvoicePaymentState
import org.btcmap.payment.PaymentInvoice
import org.btcmap.payment.observeInvoicePayment
import java.text.NumberFormat

class BoostFragment : Fragment() {

    private data class Args(val placeId: Long)

    private val args by lazy { Args(requireArguments().getLong("place_id")) }

    private var _binding: BoostFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BoostViewModel by lazy {
        ViewModelProvider(
            this,
            BoostViewModel.Factory { api().getPlaceBoostQuote() },
        )[BoostViewModel::class.java]
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
            logTag = TAG,
            onState = { render(it, payment) },
            onPaid = { parentFragmentManager.popBackStack() },
        )

        binding.btnContinue.setOnClickListener {
            val days = selectedDays()
            viewModel.order {
                val response = api().boostPlace(placeId = args.placeId, days = days)
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
            setDurationPrice(binding.boost1m, R.string.months_1, quote.quote30dsat)
            setDurationPrice(binding.boost3m, R.string.months_3, quote.quote90dsat)
            setDurationPrice(binding.boost12m, R.string.months_12, quote.quote365dsat)
        }

        // The options and continue button are locked until the quote is loaded,
        // while an order is in flight, and once an invoice exists, so a second
        // boost cannot be ordered (and charged) by tapping continue again.
        val enabled = state.actionsEnabled
        binding.boost1m.isEnabled = enabled
        binding.boost3m.isEnabled = enabled
        binding.boost12m.isEnabled = enabled
        binding.btnContinue.isEnabled = enabled

        val invoice = state.invoice
        if (invoice == null) {
            payment.hide()
        } else {
            payment.show(invoice)
        }
    }

    private fun setDurationPrice(button: RadioButton, labelRes: Int, priceSat: Long) {
        val price = getString(R.string.d_sat, NumberFormat.getNumberInstance().format(priceSat))
        button.text = getString(R.string.duration_with_price, getString(labelRes), price)
    }

    private fun selectedDays(): Long = when {
        binding.boost12m.isChecked -> 365L
        binding.boost3m.isChecked -> 90L
        else -> 30L
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val TAG = "BoostFragment"
    }
}
