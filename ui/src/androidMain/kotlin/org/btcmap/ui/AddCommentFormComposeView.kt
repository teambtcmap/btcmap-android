package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [AddCommentForm] inside the Android Views hierarchy. The app sets
 * [state] and the two callbacks; this view holds no state of its own.
 */
class AddCommentFormComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var state: AddCommentUiState? by mutableStateOf(null)

    var onRetry: () -> Unit = {}

    var onContinue: (comment: String) -> Unit = {}

    @Composable
    override fun Content() {
        val current = state ?: return
        AppTheme {
            AddCommentForm(
                state = current,
                onRetry = onRetry,
                onContinue = onContinue,
            )
        }
    }
}
