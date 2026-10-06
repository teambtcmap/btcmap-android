package org.btcmap.ui

import kotlinx.coroutines.flow.StateFlow
import org.btcmap.api.ActivityFeedItem
import org.btcmap.dbstats.BundleStats

/**
 * The actions the shared screens cannot perform themselves, because they need
 * the platform. The Android host implements them with Intents, the desktop with
 * its window; the root passes the one implementation down to every screen.
 *
 * Kept deliberately small: only the actions the migrated screens actually use
 * are declared, and more are added as screens move.
 */
interface AppPlatform {

    /**
     * Opens [url] in the platform's browser. The Android host shows a chooser,
     * as the event review queue's website rows do.
     */
    fun openUrl(url: String)

    /**
     * Opens a bolt11 invoice in the user's Lightning wallet, telling them when
     * no wallet is installed.
     */
    fun openLightningWallet(bolt11: String)

    /** Copies a bolt11 invoice to the clipboard under [label]. */
    fun copyBolt11(label: String, bolt11: String)

    /** Shows a transient confirmation that can outlive the screen that raised it. */
    fun showMessage(message: String)

    /** Opens a maps app with directions to [lat]/[lon]. */
    fun openDirections(lat: Double, lon: Double)

    /**
     * Asks the user for evidence photos (taking or choosing) and returns them
     * already encoded. Used by the report screen.
     */
    suspend fun pickPhotos(): List<ByteArray>

    /**
     * Reads the bundled snapshot assets for the database stats page. Failures
     * are reported through [showError] and the successful snapshots returned.
     */
    suspend fun loadBundles(): Map<String, BundleStats>

    /** Surfaces an error to the user, as the image stats page does on a read failure. */
    fun showError(throwable: Throwable)

    /**
     * Opens the place a feed row is about: the map for a live place, or its
     * OpenStreetMap page for a deleted one.
     */
    fun openFeedItem(item: ActivityFeedItem)

    /** Opens a place on the map, selecting it. */
    fun openPlace(placeId: Long)

    /** Shares [text] through the platform's share sheet. */
    fun shareText(text: String)

    /** Opens the dialer with [phone] pre-filled. */
    fun openDialer(phone: String?)

    /** Opens the mail app with [email] as the recipient. */
    fun openEmail(email: String?)

    /**
     * Opens [url] on btcmap.org in a Custom Tab, targeted at the browser so the
     * app's own App Link does not capture it.
     */
    fun openOnBtcmap(url: String)

    /** Whether a newer published build is available, for the map's update button. */
    val updateAvailable: StateFlow<Boolean>

    /** Opens the update dialog when a newer build was found. */
    fun showUpdateDialog()

    /** Reports the app's fully-drawn moment once the first markers have drawn. */
    fun reportFullyDrawn()
}
