package org.btcmap.boost

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.api
import org.btcmap.databinding.BoostFragmentBinding
import org.btcmap.ui.BoostScreenLabels
import org.btcmap.ui.InvoicePaymentLabels
import org.btcmap.ui.InvoicePaymentSectionLabels

/**
 * The boost screen: a toolbar over the shared [org.btcmap.ui.BoostScreen], which
 * owns the quote, the duration choices, the invoice and the payment poll. This
 * fragment supplies the labels and the wallet/clipboard actions.
 */
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

        val content = binding.boostContent
        content.api = api()
        content.placeId = args.placeId
        content.labels = boostLabels()
        content.iconTypeface = org.btcmap.util.iconTypeface
        content.onPay = { openWallet(it) }
        content.onCopy = { copyToClipboard(it) }
        content.onBack = { parentFragmentManager.popBackStack() }
        content.onPosted = {
            (activity as? Activity)?.showMessage(getString(R.string.your_boost_is_active))
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun boostLabels(): BoostScreenLabels = BoostScreenLabels(
        active = getString(R.string.your_boost_is_active),
        backToMap = getString(R.string.back_to_map),
        planLabel = { getString(it.labelRes()) },
        description = getString(R.string.boost_description),
        durationTitle = getString(R.string.boost_duration),
        continueLabel = getString(R.string.btn_continue),
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

    private fun openWallet(bolt11: String) {
        val intent = Intent(Intent.ACTION_VIEW, "lightning:$bolt11".toUri())
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            (activity as? Activity)?.showMessage(getString(R.string.you_dont_have_a_compatible_wallet))
        }
    }

    private fun copyToClipboard(bolt11: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.btc_map_boost_payment_request), bolt11)
        )
        (activity as? Activity)?.showMessage(getString(R.string.copied_to_clipboard))
    }
}

/** The Android label resource for each shared plan. */
@StringRes
private fun BoostPlan.labelRes(): Int = when (this) {
    BoostPlan.ONE_MONTH -> R.string.months_1
    BoostPlan.THREE_MONTHS -> R.string.months_3
    BoostPlan.TWELVE_MONTHS -> R.string.months_12
}
