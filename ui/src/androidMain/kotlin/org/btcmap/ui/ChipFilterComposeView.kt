package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [ChipFilterContent] so it can be set into a `MaterialAlertDialog`
 * through `setView`. The app must set the view-tree owners on it before showing
 * the dialog (a dialog's window does not inherit them).
 */
class ChipFilterComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var areasLabel: String by mutableStateOf("")

    var areaOptions: List<ChipOption> by mutableStateOf(emptyList())

    var intervalLabel: String by mutableStateOf("")

    var intervalOptions: List<ChipOption> by mutableStateOf(emptyList())

    var onAreaToggle: (key: String) -> Unit = {}

    var onIntervalSelect: (key: String) -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme {
            ChipFilterContent(
                areasLabel = areasLabel,
                areaOptions = areaOptions,
                intervalLabel = intervalLabel,
                intervalOptions = intervalOptions,
                onAreaToggle = onAreaToggle,
                onIntervalSelect = onIntervalSelect,
            )
        }
    }
}
