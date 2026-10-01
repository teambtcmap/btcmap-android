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
 * Hosts [UserProfileScreen] inside the Android Views hierarchy. The app sets
 * [state], [iconTypeface] and the callbacks; this view holds no state of its
 * own.
 */
class UserProfileComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var state: UserProfileUiState? by mutableStateOf(null)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onEditUsername: () -> Unit = {}

    var onEditPassword: () -> Unit = {}

    var onDeletePlace: (id: Long) -> Unit = {}

    var onDeleteArea: (id: Long) -> Unit = {}

    var onLogOut: () -> Unit = {}

    @Composable
    override fun Content() {
        val current = state ?: return
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            UserProfileScreen(
                state = current,
                onEditUsername = onEditUsername,
                onEditPassword = onEditPassword,
                onDeletePlace = onDeletePlace,
                onDeleteArea = onDeleteArea,
                onLogOut = onLogOut,
            )
        }
    }
}
