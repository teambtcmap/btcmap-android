package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [SettingsScreen] inside the Android Views hierarchy. The app sets
 * [items] and the two callbacks; this view holds no state of its own.
 */
class SettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var items: List<SettingsItem> by mutableStateOf(emptyList())

    var onItemClick: (key: String) -> Unit = {}

    var onItemCheckedChange: (key: String, checked: Boolean) -> Unit = { _, _ -> }

    @Composable
    override fun Content() {
        AppTheme {
            SettingsScreen(
                items = items,
                onItemClick = onItemClick,
                onItemCheckedChange = onItemCheckedChange,
            )
        }
    }
}
