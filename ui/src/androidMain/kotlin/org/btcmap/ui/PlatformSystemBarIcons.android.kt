package org.btcmap.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun PlatformSystemBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        // The Compose view is hosted by the activity, but its context can be a
        // wrapper, so unwrap to the window's activity.
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !light
            isAppearanceLightNavigationBars = !light
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
