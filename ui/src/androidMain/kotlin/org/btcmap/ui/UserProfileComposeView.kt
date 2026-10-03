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
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.Settings

/**
 * Hosts [ProfileScreen] inside the Android Views hierarchy. The app sets the
 * API, database, settings and labels; this view holds no state of its own.
 */
class UserProfileComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var api: Api? by mutableStateOf(null)

    var database: Database? by mutableStateOf(null)

    var settings: Settings? by mutableStateOf(null)

    var profileLabels: UserProfileLabels? by mutableStateOf(null)

    var formLabels: ProfileFormLabels? by mutableStateOf(null)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onLoggedOut: () -> Unit = {}

    @Composable
    override fun Content() {
        val api = api ?: return
        val database = database ?: return
        val settings = settings ?: return
        val profileLabels = profileLabels ?: return
        val formLabels = formLabels ?: return
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            ProfileScreen(
                api = api,
                db = database,
                settings = settings,
                profileLabels = profileLabels,
                formLabels = formLabels,
                onLoggedOut = onLoggedOut,
            )
        }
    }
}
