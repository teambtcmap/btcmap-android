package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
import org.btcmap.payment.InvoicePaymentViewModel
import org.btcmap.payment.PaymentInvoice
import org.btcmap.payment.invoicePaymentViewModel
import org.btcmap.payment.observeInvoicePayment
import java.text.NumberFormat

class AddCommentFragment : Fragment() {

    private data class Args(
        val placeId: Long,
        val notifyOnPosted: Boolean,
    )

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            notifyOnPosted = requireArguments().getBoolean(ARG_NOTIFY_ON_POSTED, false),
        )
    }

    private var _binding: AddCommentFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoicePaymentViewModel<CommentQuoteResponse> by lazy {
        invoicePaymentViewModel { api().getCommentQuote() }
    }

    private val addCommentViewModel: AddCommentViewModel by lazy {
        ViewModelProvider(this)[AddCommentViewModel::class.java]
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
                // Only the comments list opened from CommentsFragment waits
                // for this. A comment posted straight from the place screen
                // must not leave a result behind that a later list visit
                // would consume as a fresh payment. The posted text rides
                // along so the list can tell this comment apart from an
                // unrelated one for the same place.
                if (args.notifyOnPosted) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        Bundle().apply {
                            putString(ARG_POSTED_COMMENT, addCommentViewModel.postedComment)
                        },
                    )
                }
                parentFragmentManager.popBackStack()
            },
        )

        // Clear the validation message as soon as the user starts fixing the
        // input instead of leaving it pinned to the field.
        binding.comment.doAfterTextChanged { binding.commentInput.error = null }

        binding.btnContinue.setOnClickListener {
            val commentText = binding.comment.text.toString().trim()
            if (commentText.isEmpty()) {
                binding.commentInput.error = getString(R.string.comment_cannot_be_empty)
                return@setOnClickListener
            }

            viewModel.order {
                val response = api().addComment(
                    placeId = args.placeId,
                    comment = commentText,
                )
                addCommentViewModel.postedComment = commentText
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
            val fee = NumberFormat.getNumberInstance().format(quote.quoteSat)
            binding.fee.text = getString(R.string.d_sat, fee)
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

    companion object {
        /**
         * Fragment result set right before this screen closes once the comment
         * was paid for; `CommentsFragment` listens for it to retry its sync.
         */
        const val REQUEST_KEY = "org.btcmap.comment.posted"

        /**
         * Argument telling this screen to set [REQUEST_KEY] once the comment is
         * paid for. Only `CommentsFragment` needs it; the place screen opens
         * this screen directly and does not listen for the result.
         */
        const val ARG_NOTIFY_ON_POSTED = "notify_on_posted"

        /**
         * The posted text inside the [REQUEST_KEY] result bundle, so the list
         * can tell the paid comment apart from an unrelated one for the same
         * place. Absent when the text is unknown; the retry then falls back to
         * any new comment id.
         */
        const val ARG_POSTED_COMMENT = "posted_comment"
    }
}
