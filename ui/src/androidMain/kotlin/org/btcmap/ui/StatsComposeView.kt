package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
        val colorScheme = if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }

        StatsScreen(
            sections = sections,
            iconFont = iconTypeface?.let { FontFamily(it) },
            colorScheme = colorScheme,
        )
    }
}
