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
import org.btcmap.stats.StatsSection

/**
 * Hosts [StatsScreen] inside the Android Views hierarchy. The app sets
 * [sections] and [iconTypeface]; this view holds no state of its own.
 */
class StatsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var sections: List<StatsSection> by mutableStateOf(emptyList())

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            StatsScreen(sections = sections)
        }
    }
}
