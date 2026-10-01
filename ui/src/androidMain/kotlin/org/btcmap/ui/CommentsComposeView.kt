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
import org.btcmap.comment.CommentsAdapterItem

/**
 * Hosts [CommentsScreen] inside the Android Views hierarchy. The app sets
 * [items], [emptyMessage], [iconTypeface] and [onAddComment]; this view holds
 * no state of its own.
 */
class CommentsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var items: List<CommentsAdapterItem> by mutableStateOf(emptyList())

    var emptyMessage: String? by mutableStateOf(null)

    var addDescription: String by mutableStateOf("")

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onAddComment: () -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            CommentsScreen(
                items = items,
                emptyMessage = emptyMessage,
                addDescription = addDescription,
                onAddComment = onAddComment,
            )
        }
    }
}
