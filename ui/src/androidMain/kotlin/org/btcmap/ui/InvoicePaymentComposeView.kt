package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [InvoicePayment] inside the Android Views hierarchy. The app generates
 * the QR and sets [qr] (null hides the block), [labels] and the callbacks.
 */
class InvoicePaymentComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var qr: ImageBitmap? by mutableStateOf(null)

    var labels: InvoicePaymentLabels? by mutableStateOf(null)

    var onPay: () -> Unit = {}

    var onCopy: () -> Unit = {}

    var onStartOver: () -> Unit = {}

    @Composable
    override fun Content() {
        val currentLabels = labels ?: return
        AppTheme {
            InvoicePayment(
                qr = qr,
                labels = currentLabels,
                onPay = onPay,
                onCopy = onCopy,
                onStartOver = onStartOver,
            )
        }
    }
}
