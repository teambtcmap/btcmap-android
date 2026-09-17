package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.CommentQuoteResponse
import org.btcmap.api.addComment
import org.btcmap.api.getCommentQuote
import org.btcmap.databinding.AddCommentFragmentBinding
import org.btcmap.payment.InvoicePaymentController
import org.btcmap.payment.InvoicePaymentState
import org.btcmap.payment.PaymentInvoice
import org.btcmap.payment.observeInvoicePayment

class AddCommentFragment : Fragment() {

    private data class Args(
        val placeId: Long,
    )

    private val args by lazy {
        Args(requireArguments().getLong("place_id"))
    }

    private var _binding: AddCommentFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentViewModel by lazy {
        ViewModelProvider(
            this,
            CommentViewModel.Factory { api().getCommentQuote() },
        )[CommentViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AddCommentFragmentBinding.inflate(inflater, container, false)
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
            paymentRequestLabel = getString(R.string.btc_map_comment_payment_request),
        )

        observeInvoicePayment(
            viewModel = viewModel,
            onState = { render(it, payment) },
            onPaid = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.your_comment_has_been_posted),
                    Toast.LENGTH_LONG,
                ).show()
                parentFragmentManager.popBackStack()
            },
        )

        binding.btnContinue.setOnClickListener {
            val commentText = binding.comment.text.toString().trim()
            if (commentText.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    R.string.comment_cannot_be_empty,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.order {
                val response = api().addComment(
                    placeId = args.placeId,
                    comment = commentText,
                )
                PaymentInvoice(id = response.invoiceId, bolt11 = response.invoice)
            }
        }

        viewModel.loadQuote()
    }

    private fun render(
        state: InvoicePaymentState<CommentQuoteResponse>,
        payment: InvoicePaymentController,
    ) {
        state.quote?.let { quote ->
            binding.fee.text = getString(R.string.d_sat, quote.quoteSat.toString())
        }

        // The field stays usable while the quote loads, but is locked while the
        // order is placed and once an invoice exists.
        binding.comment.isEnabled = state.inputEnabled
        binding.btnContinue.isEnabled = state.actionsEnabled

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
