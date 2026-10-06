package org.btcmap.ui

import androidx.compose.ui.text.font.FontFamily
import coil3.ImageLoader
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.Settings
import org.btcmap.sync.SyncController
import org.btcmap.ui.map.OfflinePacks

/**
 * What the shared application root needs from the host to run its screens: the
 * app-wide data services, plus the platform-resolved values a screen cannot
 * build itself (the bundled map style, the icon font).
 *
 * The Android host builds this from its `Application`; the desktop from its
 * window. It is passed once, when the root is created.
 */
class AppServices(
    val api: Api,
    val db: Database,
    val settings: Settings,
    /** The app-scoped sync, observed by the map and the database stats page. */
    val syncController: SyncController,
    /** Coil's loader, read by the image stats page. */
    val imageLoader: ImageLoader,
    /** The data directory Coil's caches live under, for the image stats page. */
    val imageHomeDirectory: String,
    /** Names this device's session in the account's token list. */
    val authTokenLabel: String,
    /** The app-scoped offline regions, driven by the area screen. */
    val offlinePacks: OfflinePacks,
    /** The hosted URL a new offline pack is downloaded for. */
    val offlineStyleUrl: String,
    /** Whether a downloaded pack's style family still matches the selected style. */
    val offlineStyleMatches: (styleUrl: String) -> Boolean,
    /** The MapLibre style URL the screens' maps load. */
    val styleUrl: String,
    /** The bundled style JSON for [styleUrl], or null for a hosted style. */
    val styleJson: String?,
    /**
     * Whether the style is one of the OpenFreeMap styles, which carry the Noto
     * Sans Bold font the cluster counts need. False for a test style pinned by
     * the host.
     */
    val usingOpenFreeMap: Boolean,
    /** The Material Symbols typeface, or null when it is not loaded yet. */
    val iconFont: FontFamily?,
)
