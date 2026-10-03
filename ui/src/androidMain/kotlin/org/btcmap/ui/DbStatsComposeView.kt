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
import org.btcmap.db.Database
import org.btcmap.dbstats.BundleStats
import org.btcmap.settings.Settings
import org.btcmap.sync.SyncState

/**
 * Hosts the shared [DbStatsPage] inside the Android Views hierarchy, under the
 * Views top bar the screen owns. The app sets the database, the settings, the
 * sync state, the labels and the bundled-snapshot stats.
 */
class DbStatsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var database: Database? by mutableStateOf(null)

    var settings: Settings? by mutableStateOf(null)

    var syncState: SyncState by mutableStateOf(SyncState.Idle)

    var labels: DbStatsPageLabels? by mutableStateOf(null)

    var bundles: Map<String, BundleStats> by mutableStateOf(emptyMap())

    /** The Android host's toolbar carries the sync action, so no button. */
    var showSyncButton: Boolean by mutableStateOf(false)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onSync: () -> Unit = {}

    @Composable
    override fun Content() {
        val database = database ?: return
        val settings = settings ?: return
        val labels = labels ?: return

        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            DbStatsPage(
                db = database,
                settings = settings,
                syncState = syncState,
                labels = labels,
                onSync = onSync,
                bundles = bundles,
                showSyncButton = showSyncButton,
            )
        }
    }
}
