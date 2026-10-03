package org.btcmap.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.AddCommentFragmentBinding
import org.btcmap.ui.AddCommentLabels
import org.btcmap.ui.CommentScreenLabels
import org.btcmap.ui.InvoicePaymentLabels
import org.btcmap.ui.InvoicePaymentSectionLabels
import org.btcmap.util.copyBolt11
import org.btcmap.util.openLightningWallet

/**
 * The add-comment screen: a toolbar over the shared
 * [org.btcmap.ui.CommentScreen], which owns the form, the fee quote, the invoice
 * and the payment poll. This fragment supplies the labels and the
 * wallet/clipboard actions.
 */
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

        val content = binding.commentContent
        content.api = api()
        content.placeId = args.placeId
        content.labels = commentLabels()
        content.iconTypeface = org.btcmap.util.iconTypeface
        content.onPay = { openLightningWallet(it) }
        content.onCopy = { copyBolt11(getString(R.string.btc_map_comment_payment_request), it) }
        content.onBack = { parentFragmentManager.popBackStack() }
        content.onPosted = {
            (activity as? Activity)?.showMessage(getString(R.string.your_comment_has_been_posted))
            // Only the comments list opened from CommentsFragment waits for
            // this; a comment posted straight from the place screen must not
            // leave a result behind that a later list visit would consume.
            if (args.notifyOnPosted) {
                parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
            }
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun commentLabels(): CommentScreenLabels = CommentScreenLabels(
        posted = getString(R.string.your_comment_has_been_posted),
        backToMap = getString(R.string.back_to_map),
        form = AddCommentLabels(
            disclosure = getString(R.string.add_element_comment_disclosure_1),
            currentFee = getString(R.string.current_fee),
            comment = getString(R.string.comment),
            placeholder = getString(R.string.comment_placeholder),
            continueLabel = getString(R.string.btn_continue),
            emptyComment = getString(R.string.comment_cannot_be_empty),
            failedToLoad = getString(R.string.failed_to_load),
            tapToRetry = getString(R.string.tap_to_retry),
        ),
        invoice = invoiceLabels(),
    )

    private fun invoiceLabels(): InvoicePaymentSectionLabels = InvoicePaymentSectionLabels(
        invoice = InvoicePaymentLabels(
            qrDescription = getString(R.string.qr_code),
            pay = getString(R.string.pay),
            copy = getString(android.R.string.copy),
            startOver = getString(R.string.start_over),
        ),
        discardMessage = getString(R.string.discard_invoice_confirmation),
        discard = getString(R.string.start_over),
        cancel = getString(android.R.string.cancel),
    )

    companion object {
        /**
         * Fragment result set right before this screen closes once the comment
         * was paid for; `CommentsFragment` listens for it to re-run its sync.
         */
        const val REQUEST_KEY = "org.btcmap.comment.posted"

        /**
         * Argument telling this screen to set [REQUEST_KEY] once the comment is
         * paid for. Only `CommentsFragment` needs it; the place screen opens
         * this screen directly and does not listen for the result.
         */
        const val ARG_NOTIFY_ON_POSTED = "notify_on_posted"
    }
}
