package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.ui.map.bundledStyleJsonFor

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

    var imagesLabels: UploadedImagesLabels? by mutableStateOf(null)

    var eventsLabels: MyEventsLabels? by mutableStateOf(null)

    /** The map style the my-events map previews render with. */
    var mapStyleUrl: String by mutableStateOf("")

    /**
     * Whether the profile is showing the uploaded-images sub-screen. The host's
     * back arrow reads it to return to the profile page before leaving it.
     */
    var uploadedImagesOpen: Boolean by mutableStateOf(false)

    /**
     * Whether the profile is showing the my-events sub-screen, read by the host's
     * back arrow the same way as [uploadedImagesOpen].
     */
    var myEventsOpen: Boolean by mutableStateOf(false)

    var iconTypeface: Typeface? by mutableStateOf(null)

    /** Opens the add-event screen pre-filled from one of the user's events. */
    var onDuplicateEvent: (MyEventUi) -> Unit = {}

    var onLoggedOut: () -> Unit = {}

    @Composable
    override fun Content() {
        val api = api ?: return
        val database = database ?: return
        val settings = settings ?: return
        val profileLabels = profileLabels ?: return
        val formLabels = formLabels ?: return
        val imagesLabels = imagesLabels ?: return
        val eventsLabels = eventsLabels ?: return
        val mapStyleJson = remember(mapStyleUrl) {
            mapStyleUrl.takeIf { it.isNotEmpty() }?.let { bundledStyleJsonFor(context, it) }
        }
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            ProfileScreen(
                api = api,
                db = database,
                settings = settings,
                profileLabels = profileLabels,
                formLabels = formLabels,
                imagesLabels = imagesLabels,
                eventsLabels = eventsLabels,
                mapStyleUrl = mapStyleUrl,
                mapStyleJson = mapStyleJson,
                showUploadedImages = uploadedImagesOpen,
                onShowUploadedImagesChange = { uploadedImagesOpen = it },
                showMyEvents = myEventsOpen,
                onShowMyEventsChange = { myEventsOpen = it },
                onDuplicateEvent = onDuplicateEvent,
                onLoggedOut = onLoggedOut,
            )
        }
    }
}
