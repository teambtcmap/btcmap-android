package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [AuthChooserContent] so it can be set into a `MaterialAlertDialog`
 * through `setView`. The app must set the view-tree owners on it before showing
 * the dialog.
 */
class AuthChooserComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var options: List<AuthChooserOption> by mutableStateOf(emptyList())

    var onSelect: (key: String) -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme {
            AuthChooserContent(options = options, onSelect = onSelect)
        }
    }
}
