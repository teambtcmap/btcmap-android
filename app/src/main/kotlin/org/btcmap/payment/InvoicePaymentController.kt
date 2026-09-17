package org.btcmap.payment

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.btcmap.R

/**
 * Renders a Lightning invoice and wires its pay and copy actions.
 *
 * The merchant boost and comment flows show the same invoice UI, so the QR
 * generation, wallet intent and clipboard handling live here instead of being
 * duplicated by each caller.
 */
internal class InvoicePaymentController(
    private val fragment: Fragment,
    private val qr: ImageView,
    private val payButton: Button,
    private val copyButton: Button,
    private val paymentRequestLabel: String,
) {

    fun show(invoice: PaymentInvoice) {
        val qrEncoder = QRGEncoder(invoice.bolt11, null, QRGContents.Type.TEXT, QR_SIZE)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE

        qr.isVisible = true
        qr.setImageBitmap(qrEncoder.getBitmap(0))

        payButton.isVisible = true
        payButton.setOnClickListener { openWallet(invoice.bolt11) }

        copyButton.isVisible = true
        copyButton.setOnClickListener { copyToClipboard(invoice.bolt11) }
    }

    fun hide() {
        qr.isVisible = false
        qr.setImageDrawable(null)
        payButton.isVisible = false
        copyButton.isVisible = false
    }

    private fun openWallet(invoice: String) {
        val intent = Intent(Intent.ACTION_VIEW, "lightning:$invoice".toUri())
        try {
            fragment.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                fragment.requireContext(),
                R.string.you_dont_have_a_compatible_wallet,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun copyToClipboard(invoice: String) {
        val clipboard =
            fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(paymentRequestLabel, invoice))
        Toast.makeText(fragment.requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
            .show()
    }

    private companion object {
        const val QR_SIZE = 1000
    }
}
