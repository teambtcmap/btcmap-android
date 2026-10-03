package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.db.Database
import org.btcmap.settings.Settings

/**
 * Hosts [SettingsPage] inside the Android Views hierarchy. The app sets
 * [settings], [database] and [labels] plus the navigation callbacks; this view
 * holds no state of its own. [reloadKey] lets the app re-read the account row
 * after a sign-in.
 */
class SettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var settings: Settings? by mutableStateOf(null)

    var database: Database? by mutableStateOf(null)

    var labels: SettingsPageLabels? by mutableStateOf(null)

    var reloadKey: Int by mutableStateOf(0)

    var onOpenAccount: () -> Unit = {}

    var onOpenColors: () -> Unit = {}

    var onOpenDbStats: () -> Unit = {}

    var onOpenImageStats: () -> Unit = {}

    @Composable
    override fun Content() {
        val settings = this.settings ?: return
        val database = this.database ?: return
        val labels = this.labels ?: return
        AppTheme {
            SettingsPage(
                settings = settings,
                db = database,
                labels = labels,
                includeImageStats = true,
                onOpenAccount = onOpenAccount,
                onOpenColors = onOpenColors,
                onOpenDbStats = onOpenDbStats,
                onOpenImageStats = onOpenImageStats,
                reloadKey = reloadKey,
            )
        }
    }
}
