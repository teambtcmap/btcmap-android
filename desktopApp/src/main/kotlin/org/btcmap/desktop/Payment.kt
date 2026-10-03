package org.btcmap.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import org.btcmap.payment.PaymentInvoice
import org.btcmap.ui.InvoicePayment
import org.btcmap.ui.InvoicePaymentLabels
import java.awt.image.BufferedImage
import java.text.NumberFormat

/** Test tags for the payment section's start-over confirmation. */
internal const val PAYMENT_DISCARD_TAG = "payment-discard"

private const val QR_SIZE = 512

private val INVOICE_LABELS = InvoicePaymentLabels(
    qrDescription = "Lightning invoice QR code",
    pay = "Pay",
    copy = "Copy",
    startOver = "Start over",
)

/**
 * The shared Lightning invoice block, with the desktop's QR, wallet and
 * clipboard handling. Starting over asks first, because a discarded invoice
 * stays payable until it expires, so paying it after ordering another would
 * charge twice.
 */
@Composable
internal fun InvoicePaymentSection(
    invoice: PaymentInvoice,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qr = remember(invoice.bolt11) { generateQrBitmap(invoice.bolt11) }
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Discard this invoice? It stays payable until it expires, so " +
                    "starting over and paying the old one too would charge you twice.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Button(
                    onClick = {
                        confirming = false
                        onStartOver()
                    },
                    modifier = Modifier.testTag(PAYMENT_DISCARD_TAG),
                ) {
                    Text("Discard")
                }
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        }
        return
    }

    InvoicePayment(
        qr = qr,
        labels = INVOICE_LABELS,
        onPay = { openUrl("lightning:${invoice.bolt11}") },
        onCopy = { copyToClipboard(invoice.bolt11) },
        onStartOver = { confirming = true },
        modifier = modifier,
    )
}

/** Encodes [text] as a QR bitmap, black on white with a one-module margin. */
internal fun generateQrBitmap(text: String, size: Int = QR_SIZE): ImageBitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until size) {
        for (y in 0 until size) {
            image.setRGB(x, y, if (matrix.get(x, y)) 0x000000 else 0xFFFFFF)
        }
    }
    return image.toComposeImageBitmap()
}

/** A sat amount with thousands separators, as the Android `d_sat` string shows. */
internal fun formatSat(sat: Long): String =
    "${NumberFormat.getNumberInstance().format(sat)} sat"
