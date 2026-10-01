package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.btcmap.Activity
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
import org.btcmap.ui.AddCommentLabels
import org.btcmap.ui.AddCommentUiState
import java.text.NumberFormat

class AddCommentFragment : Fragment() {

    private data class Args(
        val placeId: Long,
        val placeName: String?,
        val notifyOnPosted: Boolean,
    )

    private val args by lazy {
        Args(
            placeId = requireArguments().getLong("place_id"),
            placeName = requireArguments().getString("place_name"),
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

        // The place name tells the user which place they are commenting on;
        // the static title is the fallback when it was not passed along.
        args.placeName?.let { binding.topAppBar.title = it }

        val payment = InvoicePaymentController(
            fragment = this,
            view = binding.invoicePayment,
            paymentRequestLabel = getString(R.string.btc_map_comment_payment_request),
            onStartOver = viewModel::startOver,
        )

        observeInvoicePayment(
            viewModel = viewModel,
            onState = { render(it, payment) },
            onPaid = {
                (activity as? Activity)?.showMessage(
                    getString(R.string.your_comment_has_been_posted),
                )
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
            // A failed quote is rendered inline with a retry instead of
            // closing the screen, since retrying is the only thing it can do.
            onQuoteFailure = {},
        )

        binding.addCommentForm.onRetry = { viewModel.loadQuote() }

        binding.addCommentForm.onContinue = { commentText ->
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
        val quote = state.quote
        val quoteFailed = quote == null && !state.loadingQuote && state.invoice == null

        _binding?.addCommentForm?.state = AddCommentUiState(
            quote = quote?.let {
                getString(R.string.d_sat, NumberFormat.getNumberInstance().format(it.quoteSat))
            },
            loadingQuote = state.loadingQuote,
            quoteFailed = quoteFailed,
            inputEnabled = state.inputEnabled,
            actionsEnabled = state.actionsEnabled,
            ordering = state.ordering,
            // The invoice block replaces the order controls, so the continue
            // button that started the order is hidden once one exists.
            showContinue = state.invoice == null,
            labels = AddCommentLabels(
                disclosure = getString(R.string.add_element_comment_disclosure_1),
                currentFee = getString(R.string.current_fee),
                comment = getString(R.string.comment),
                placeholder = getString(R.string.comment_placeholder),
                continueLabel = getString(R.string.btn_continue),
                emptyComment = getString(R.string.comment_cannot_be_empty),
                failedToLoad = getString(R.string.failed_to_load),
                tapToRetry = getString(R.string.tap_to_retry),
            ),
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
