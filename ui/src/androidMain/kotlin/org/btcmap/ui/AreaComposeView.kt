package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.GetEventsItem
import org.btcmap.area.AreaIssues
import org.btcmap.area.AreaPlaceIssue
import org.btcmap.db.table.place.Place
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds

/**
 * Hosts the shared [AreaScreen] body inside the Android Views hierarchy, under
 * the collapsing toolbar the area screen owns. The app sets the loaded sections,
 * the offline state and the dialog inputs; this view holds no state of its own.
 */
class AreaComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var description: String? by mutableStateOf(null)

    var websiteText: String? by mutableStateOf(null)

    var boostedMerchants: List<Place> by mutableStateOf(emptyList())

    var events: List<GetEventsItem> by mutableStateOf(emptyList())

    var issues: AreaIssues? by mutableStateOf(null)

    var offlineState: OfflineAreaState? by mutableStateOf(null)

    /** Whether a downloaded pack's style family still matches the selected one. */
    var offlineStyleMatches: (String) -> Boolean = { true }

    var strings: AreaStrings? by mutableStateOf(null)

    var boostedMarkerColor: Color by mutableStateOf(Color(0xFFF7931A))

    var iconTypeface: Typeface? by mutableStateOf(null)

    /** The inputs the offline download dialog needs; shown while the flag is set. */
    var areaName: String by mutableStateOf("")

    var offlineStyleName: String by mutableStateOf("")

    var offlineBounds: OfflineBounds? by mutableStateOf(null)

    var showOfflineDialog: Boolean by mutableStateOf(false)

    var onOpenPlace: (Long) -> Unit = {}

    var onOpenEvent: (GetEventsItem) -> Unit = {}

    var onOpenIssue: (AreaPlaceIssue) -> Unit = {}

    var onJoinUs: () -> Unit = {}

    var onDownload: () -> Unit = {}

    var onDelete: () -> Unit = {}

    var onDismissOfflineDialog: () -> Unit = {}

    var onConfirmOfflineDownload: (Int) -> Unit = {}

    @Composable
    override fun Content() {
        val strings = strings ?: return

        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val bounds = offlineBounds
        val dialog = if (showOfflineDialog && bounds != null) {
            AreaOfflineDialog(areaName = areaName, styleName = offlineStyleName, bounds = bounds)
        } else {
            null
        }

        AppTheme(iconFont = fontFamily) {
            AreaScreen(
                description = description,
                websiteText = websiteText,
                boostedMerchants = boostedMerchants,
                events = events,
                issues = issues,
                offlineState = offlineState,
                offlineStyleMatches = offlineStyleMatches,
                strings = strings,
                onOpenPlace = onOpenPlace,
                onOpenEvent = onOpenEvent,
                onOpenIssue = onOpenIssue,
                onJoinUs = onJoinUs,
                onDownload = onDownload,
                onDelete = onDelete,
                offlineDialog = dialog,
                onDismissOfflineDialog = onDismissOfflineDialog,
                onConfirmOfflineDownload = onConfirmOfflineDownload,
                boostedMarkerColor = boostedMarkerColor,
            )
        }
    }
}
