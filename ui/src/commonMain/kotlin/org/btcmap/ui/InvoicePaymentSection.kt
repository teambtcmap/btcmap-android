package org.btcmap.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.btcmap.payment.PaymentInvoice

/** Test tag for the start-over confirmation's discard button. */
const val PAYMENT_DISCARD_TAG = "payment-discard"

/** The invoice block's strings plus its discard confirmation. */
data class InvoicePaymentSectionLabels(
    val invoice: InvoicePaymentLabels,
    val discardMessage: String,
    val discard: String,
    val cancel: String,
)

/**
 * The shared Lightning invoice block over an [invoice]: the QR code (generated
 * here, in common), the pay and copy actions, and a start-over that asks first.
 *
 * The host supplies [onPay] and [onCopy] with the invoice's bolt11 string,
 * because opening a wallet and writing to the clipboard are platform calls. A
 * discarded invoice stays payable until it expires, so paying it after ordering
 * another would charge twice, hence the confirmation.
 */
@Composable
fun InvoicePaymentSection(
    invoice: PaymentInvoice,
    labels: InvoicePaymentSectionLabels,
    onPay: (bolt11: String) -> Unit,
    onCopy: (bolt11: String) -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
    qrSize: Int = 512,
) {
    val qr = remember(invoice.bolt11, qrSize) { qrBitmap(invoice.bolt11, qrSize) }
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = labels.discardMessage,
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
                    Text(labels.discard)
                }
                TextButton(onClick = { confirming = false }) { Text(labels.cancel) }
            }
        }
        return
    }

    InvoicePayment(
        qr = qr,
        labels = labels.invoice,
        onPay = { onPay(invoice.bolt11) },
        onCopy = { onCopy(invoice.bolt11) },
        onStartOver = { confirming = true },
        modifier = modifier,
    )
}
