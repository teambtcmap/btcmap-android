package org.btcmap.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/**
 * The Material Symbols typeface used to render icon ligatures, or null when the
 * platform has not supplied one. Provided by [AppTheme].
 */
val LocalIconFont = staticCompositionLocalOf<FontFamily?> { null }

/**
 * The app-wide Material 3 theme.
 *
 * The color scheme comes from the platform — dynamic (Material You) colors on
 * Android, the system light/dark on desktop — and [iconFont] carries the
 * Material Symbols typeface the icon ligatures are drawn with.
 */
@Composable
fun AppTheme(
    iconFont: FontFamily? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalIconFont provides iconFont) {
        MaterialTheme(colorScheme = platformColorScheme(), content = content)
    }
}

/** The color scheme for the current platform and light/dark setting. */
@Composable
expect fun platformColorScheme(): ColorScheme
