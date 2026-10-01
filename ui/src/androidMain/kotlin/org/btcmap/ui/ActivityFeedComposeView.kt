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

/**
 * Hosts [ActivityFeedScreen] inside the Android Views hierarchy. The app sets
 * [state], [iconTypeface] and the callbacks; this view holds no state of its
 * own.
 */
class ActivityFeedComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var state: ActivityFeedState by mutableStateOf(ActivityFeedState.Loading)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onItemClick: (key: String) -> Unit = {}

    var onRetry: () -> Unit = {}

    /** The empty/error message currently shown, or null while loading or content. */
    val emptyMessage: String?
        get() = (state as? ActivityFeedState.Empty)?.message

    /** Whether the loading indicator is showing. */
    val loading: Boolean
        get() = state is ActivityFeedState.Loading

    /** The rows currently shown, empty when not in the content state. */
    val rows: List<ActivityFeedRow>
        get() = (state as? ActivityFeedState.Content)?.rows.orEmpty()

    @Composable
    override fun Content() {
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            ActivityFeedScreen(
                state = state,
                onItemClick = onItemClick,
                onRetry = onRetry,
            )
        }
    }
}
