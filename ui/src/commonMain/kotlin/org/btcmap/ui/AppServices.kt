package org.btcmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import coil3.ImageLoader
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.settings.MapStyle
import org.btcmap.settings.Settings
import org.btcmap.settings.mapStyle
import org.btcmap.sync.SyncController
import org.btcmap.ui.map.OfflinePacks

/**
 * A MapLibre style: a bundled one handed over as JSON, or a hosted URL that the
 * map loads itself.
 */
data class MapStyleSpec(val url: String, val json: String?)

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
    /** Resolves the hosted URL a new offline pack is downloaded for. */
    val offlineStyleUrlFor: (style: MapStyle) -> String,
    /**
     * Whether a downloaded pack's style family still matches the one selected
     * for [selectedStyleUrl], so a pack from a style the user has since left is
     * reported as a mismatch.
     */
    val offlineStyleMatches: (selectedStyleUrl: String, downloaded: String) -> Boolean,
    /** Resolves the style the screens' maps load, reading the host's bundled JSON. */
    val styleFor: (style: MapStyle) -> MapStyleSpec,
    /**
     * Whether the style is one of the OpenFreeMap styles, which carry the Noto
     * Sans Bold font the cluster counts need. False for a test style pinned by
     * the host.
     */
    val usingOpenFreeMap: Boolean,
    /** The Material Symbols typeface, or null when it is not loaded yet. */
    val iconFont: FontFamily?,
)

/**
 * The style the maps load for the current setting, resolved through
 * [AppServices.styleFor]. Reading the setting here, rather than baking the
 * resolved style into [AppServices], is what lets a change in the settings
 * screen reach the map: the map screen is recreated when the user navigates
 * back to it, so it resolves the newly picked style.
 */
@Composable
fun AppServices.rememberMapStyle(): MapStyleSpec {
    val style = settings.mapStyle
    return remember(style) { styleFor(style) }
}
