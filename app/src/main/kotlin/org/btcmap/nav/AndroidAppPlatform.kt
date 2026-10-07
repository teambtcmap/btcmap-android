package org.btcmap.nav

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.source
import org.btcmap.App
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getPlaceOsmId
import org.btcmap.bundle.BundledAreas
import org.btcmap.bundle.BundledComments
import org.btcmap.bundle.BundledEvents
import org.btcmap.bundle.BundledPlaces
import org.btcmap.db.table.area.TABLE as AREA_TABLE
import org.btcmap.db.table.comment.TABLE as COMMENT_TABLE
import org.btcmap.db.table.event.TABLE as EVENT_TABLE
import org.btcmap.db.table.place.TABLE as PLACE_TABLE
import org.btcmap.dbstats.BundleStats
import org.btcmap.dbstats.readBundles
import org.btcmap.i18n.Strings
import org.btcmap.map.UpdateNotificationController
import org.btcmap.place.toOsmUrl
import org.btcmap.ui.AppPlatform
import org.btcmap.util.userFacingMessage

/** The bundled snapshots the database stats page reads, keyed by table name. */
private val DATABASE_BUNDLES = mapOf(
    PLACE_TABLE to BundledPlaces.FILE_NAME,
    COMMENT_TABLE to BundledComments.FILE_NAME,
    AREA_TABLE to BundledAreas.FILE_NAME,
    EVENT_TABLE to BundledEvents.FILE_NAME,
)

/** The Android implementation of the shared [AppPlatform]. */
class AndroidAppPlatform(
    private val activity: org.btcmap.Activity,
    private val photoPicker: suspend () -> List<ByteArray>,
    private val openPlaceAction: (Long) -> Unit,
) : AppPlatform {

    private val availableUpdate = MutableStateFlow(false)

    private val updateController = UpdateNotificationController(
        context = activity,
        lifecycleOwner = activity,
        onUpdateAvailable = { availableUpdate.value = it },
    )

    override val updateAvailable: StateFlow<Boolean> = availableUpdate

    override fun showUpdateDialog() {
        updateController.showDialog()
    }

    override fun reportFullyDrawn() {
        activity.reportFullyDrawn()
    }

    override fun openUrl(url: String) {
        activity.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_VIEW, url.toUri()), null),
        )
    }

    override fun openLightningWallet(bolt11: String) {
        val intent = Intent(Intent.ACTION_VIEW, "lightning:$bolt11".toUri())
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            activity.showMessage(Strings.current()["you_dont_have_a_compatible_wallet"])
        }
    }

    override fun copyBolt11(label: String, bolt11: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, bolt11))
        activity.showMessage(Strings.current()["copied_to_clipboard"])
    }

    override fun showMessage(message: String) {
        activity.showMessage(message)
    }

    override fun openPlace(placeId: Long) {
        openPlaceAction(placeId)
    }

    override fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        activity.startActivity(Intent.createChooser(intent, null))
    }

    override fun openDialer(phone: String?) {
        val number = phone?.trim()
        if (number.isNullOrEmpty()) return
        activity.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    }

    override fun openEmail(email: String?) {
        val address = email?.trim()
        if (address.isNullOrEmpty()) return
        activity.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri()))
    }

    /**
     * Targets the browser explicitly: a plain VIEW intent would be captured by
     * this app's own verified App Link for btcmap.org/merchant and reopen the
     * place instead of the site. Falls back to a chooser that excludes this app.
     */
    override fun openOnBtcmap(url: String) {
        val uri = url.toUri()

        CustomTabsClient.getPackageName(activity, null)?.let { browser ->
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(browser)
            try {
                customTabs.launchUrl(activity, uri)
                return
            } catch (_: ActivityNotFoundException) {
                // Fall through to the chooser below.
            }
        }

        val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), null)
        chooser.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(activity, org.btcmap.Activity::class.java)),
        )
        try {
            activity.startActivity(chooser)
        } catch (t: Throwable) {
            showError(t)
        }
    }

    override fun openDirections(lat: Double, lon: Double) {
        val coordinates = "$lat,$lon"
        val uri = "geo:$coordinates?q=$coordinates".toUri()
        activity.startActivity(
            Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), null),
        )
    }

    override suspend fun pickPhotos(): List<ByteArray> = photoPicker()

    override suspend fun loadBundles(): Map<String, BundleStats> = withContext(Dispatchers.IO) {
        val reads = readBundles(DATABASE_BUNDLES) { fileName ->
            runCatching { activity.assets.open(fileName).source() }.getOrNull()
        }
        reads.failures.forEach(::showError)
        reads.stats
    }

    override fun showError(throwable: Throwable) {
        Toast.makeText(
            activity,
            throwable.userFacingMessage(Strings.current()["error"]),
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun openFeedItem(item: ActivityFeedItem) {
        if (item.type == ActivityFeedItem.TYPE_PLACE_DELETED) {
            openDeletedPlace(item.placeId)
        }
    }

    /**
     * A deleted place has no screen to open, so its feed row opens the
     * OpenStreetMap page instead. The OSM id comes from the local tombstone when
     * present, and from the server otherwise.
     */
    private fun openDeletedPlace(placeId: Long) {
        activity.lifecycleScope.launch {
            val app = activity.applicationContext as App
            val osmId = withContext(Dispatchers.IO) {
                app.db.place.selectByIdIncludingDeleted(placeId)?.osmId
                    ?: try {
                        app.api.getPlaceOsmId(placeId)
                    } catch (t: Throwable) {
                        null
                    }
            } ?: return@launch

            val url = osmId.toOsmUrl() ?: return@launch
            activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }
}
