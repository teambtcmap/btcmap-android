package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.Api

/**
 * Hosts [CommentScreen] inside the Android Views hierarchy. The app sets the
 * API, the place, the labels and the pay/copy/back callbacks; this view holds no
 * state of its own.
 */
class CommentComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var api: Api? by mutableStateOf(null)

    var placeId: Long by mutableStateOf(0L)

    var labels: CommentScreenLabels? by mutableStateOf(null)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onPay: (bolt11: String) -> Unit = {}

    var onCopy: (bolt11: String) -> Unit = {}

    var onBack: () -> Unit = {}

    var onPosted: () -> Unit = {}

    @Composable
    override fun Content() {
        val api = api ?: return
        val labels = labels ?: return
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            CommentScreen(
                api = api,
                placeId = placeId,
                labels = labels,
                onPay = onPay,
                onCopy = onCopy,
                onBack = onBack,
                onPosted = onPosted,
            )
        }
    }
}
