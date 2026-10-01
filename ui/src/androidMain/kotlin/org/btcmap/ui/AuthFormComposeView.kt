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
 * Hosts [AuthFormContent] so it can be set into a `MaterialAlertDialog` through
 * `setView`. The app must set the view-tree owners on it before showing the
 * dialog.
 */
class AuthFormComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var fields: List<AuthField> by mutableStateOf(emptyList())

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onValueChange: (key: String, value: String) -> Unit = { _, _ -> }

    var onDone: () -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            AuthFormContent(
                fields = fields,
                onValueChange = onValueChange,
                onDone = onDone,
            )
        }
    }
}
