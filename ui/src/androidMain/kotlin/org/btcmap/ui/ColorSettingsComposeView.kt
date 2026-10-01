package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [ColorSettingsScreen] inside the Android Views hierarchy. The app sets
 * [items] and [onItemClick]; this view holds no state of its own.
 */
class ColorSettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var items: List<ColorItem> by mutableStateOf(emptyList())

    var onItemClick: (key: String) -> Unit = {}

    @Composable
    override fun Content() {
        AppTheme {
            ColorSettingsScreen(items = items, onItemClick = onItemClick)
        }
    }
}
