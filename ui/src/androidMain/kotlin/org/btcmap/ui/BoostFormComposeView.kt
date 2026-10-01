package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [BoostForm] inside the Android Views hierarchy. The app sets [state]
 * and [onContinue]; this view holds no state of its own.
 */
class BoostFormComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var state: BoostFormUiState? by mutableStateOf(null)

    var onContinue: (key: String) -> Unit = {}

    @Composable
    override fun Content() {
        val current = state ?: return
        AppTheme {
            BoostForm(state = current, onContinue = onContinue)
        }
    }
}
