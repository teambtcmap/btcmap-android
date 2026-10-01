package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [RadioPickerContent] so it can be set into a `MaterialAlertDialog`
 * through `setView`. The app must set the view-tree lifecycle and saved-state
 * owners on it before showing the dialog (a dialog's window does not inherit
 * them).
 */
class RadioPickerComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var options: List<RadioOption> by mutableStateOf(emptyList())

    var selectedKey: String? by mutableStateOf(null)

    var onSelect: (key: String) -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme {
            RadioPickerContent(
                options = options,
                selectedKey = selectedKey,
                onSelect = onSelect,
            )
        }
    }
}
