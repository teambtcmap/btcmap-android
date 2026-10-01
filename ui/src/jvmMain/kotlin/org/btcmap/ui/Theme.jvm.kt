package org.btcmap.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(): ColorScheme =
    if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
