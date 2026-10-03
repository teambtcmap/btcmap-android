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
import org.btcmap.api.ActivityFeedItem

/**
 * Hosts the shared [ActivityFeedPage] inside the Android Views hierarchy: the
 * app supplies the suspend [load], the row mapping [toRow] and the messages;
 * the page owns the load state machine. [reloadKey] re-runs the load (a filter
 * change, or a resume on the Saved tab).
 */
class ActivityFeedComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var load: (suspend () -> List<ActivityFeedItem>)? by mutableStateOf(null)

    var toRow: ((ActivityFeedItem) -> ActivityFeedRow)? by mutableStateOf(null)

    /** Resolved after the load, so a scope-dependent message reads fresh state. */
    var emptyMessage: (() -> String)? by mutableStateOf(null)

    var errorMessage: String by mutableStateOf("")

    var reloadKey: Int by mutableStateOf(0)

    var onItemClick: (ActivityFeedItem) -> Unit = {}

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val load = load ?: return
        val toRow = toRow ?: return
        val emptyMessage = emptyMessage ?: return

        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            ActivityFeedPage(
                load = load,
                toRow = toRow,
                emptyMessage = emptyMessage,
                errorMessage = errorMessage,
                onItemClick = onItemClick,
                reloadKey = reloadKey,
            )
        }
    }
}
