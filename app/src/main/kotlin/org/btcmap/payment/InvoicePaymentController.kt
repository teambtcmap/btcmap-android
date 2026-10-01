package org.btcmap.payment

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.btcmap.Activity
import org.btcmap.R
import org.btcmap.ui.InvoicePaymentComposeView
import org.btcmap.ui.InvoicePaymentLabels

/**
 * Renders a Lightning invoice and wires its pay, copy and start-over actions.
 *
 * The merchant boost and comment flows show the same invoice UI, so the QR
 * generation, wallet intent and clipboard handling live here instead of being
 * duplicated by each caller. The QR is generated here (Android only) and handed
 * to the shared [InvoicePaymentComposeView].
 */
internal class InvoicePaymentController(
    private val fragment: Fragment,
    private val view: InvoicePaymentComposeView,
    private val paymentRequestLabel: String,
    private val onStartOver: () -> Unit,
) {

    /** The invoice whose QR is currently drawn, so a render that changes
     *  nothing else does not re-encode it on the main thread. */
    private var shownInvoice: String? = null

    fun show(invoice: PaymentInvoice) {
        view.labels = InvoicePaymentLabels(
            qrDescription = fragment.getString(R.string.qr_code),
            pay = fragment.getString(R.string.pay),
            copy = fragment.getString(android.R.string.copy),
            startOver = fragment.getString(R.string.start_over),
        )
        view.onPay = { openWallet(invoice.bolt11) }
        view.onCopy = { copyToClipboard(invoice.bolt11) }
        view.onStartOver = { confirmStartOver() }

        if (shownInvoice != invoice.bolt11) {
            view.qr = generateQr(invoice.bolt11)
            shownInvoice = invoice.bolt11
        }
    }

    fun hide() {
        view.qr = null
        shownInvoice = null
    }

    private fun generateQr(invoice: String): ImageBitmap {
        val qrEncoder = QRGEncoder(invoice, null, QRGContents.Type.TEXT, QR_SIZE)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE
        return qrEncoder.getBitmap(0).asImageBitmap()
    }

    /**
     * Confirms before discarding an invoice. There is no endpoint to cancel it,
     * so it stays payable until it expires: starting over and then paying the
     * old invoice as well would charge the user twice.
     */
    private fun confirmStartOver() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setMessage(R.string.discard_invoice_confirmation)
            .setPositiveButton(R.string.start_over) { _, _ -> onStartOver() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openWallet(invoice: String) {
        val intent = Intent(Intent.ACTION_VIEW, "lightning:$invoice".toUri())
        try {
            fragment.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showMessage(R.string.you_dont_have_a_compatible_wallet)
        }
    }

    private fun copyToClipboard(invoice: String) {
        val clipboard =
            fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(paymentRequestLabel, invoice))
        showMessage(R.string.copied_to_clipboard)
    }

    /**
     * Routed through the activity so the message is not tied to this screen's
     * view, which a payment that closes it would take away.
     */
    private fun showMessage(messageRes: Int) {
        (fragment.activity as? Activity)?.showMessage(fragment.getString(messageRes))
    }

    private companion object {
        const val QR_SIZE = 1000
    }
}
