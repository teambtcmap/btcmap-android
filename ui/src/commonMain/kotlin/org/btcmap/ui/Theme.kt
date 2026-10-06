package org.btcmap.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
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
 * The colour scheme is the app's own, generated from the brand seed (see
 * [btcmapColorScheme]), so Android and the desktop render the same colours in
 * both modes rather than the platform's dynamic palette. [iconFont] carries the
 * Material Symbols typeface the icon ligatures are drawn with.
 */
@Composable
fun AppTheme(
    iconFont: FontFamily? = null,
    /**
     * Forces the light or dark scheme instead of the platform's. The desktop's
     * headless screenshot has no system theme to read, so it passes this.
     */
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val scheme = btcmapColorScheme(darkTheme ?: isSystemInDarkTheme())

    CompositionLocalProvider(LocalIconFont provides iconFont) {
        MaterialTheme(colorScheme = scheme) {
            // MaterialTheme does not provide a content color, so a bare Text
            // would fall back to black and vanish on a dark background. Set it
            // to the scheme's onBackground for the whole tree.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                content = content,
            )
        }
    }
}
