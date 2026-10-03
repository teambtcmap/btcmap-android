package org.btcmap.map

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.api.apiHttpClient
import org.btcmap.update.AvailableUpdate
import org.btcmap.update.UpdateCheck
import org.btcmap.util.rethrowIfCancellation

/**
 * Reports whether the published APK is newer than this build, and opens the
 * update dialog when the map's update button is tapped.
 *
 * The check is shared [UpdateCheck] logic; this only supplies the app's build
 * numbers and manifest URL, hands the button's visibility to the shared map
 * through [onUpdateAvailable], and shows the dialog. The check is process-scoped:
 * its outcome is cached and reused, so a rotation or a return to the map does
 * not re-issue it. The HTTP client is shared for the whole process.
 */
class UpdateNotificationController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onUpdateAvailable: (Boolean) -> Unit,
) {
    init {
        onUpdateAvailable(false)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.withResumed {
                launch {
                    if (checked) {
                        onUpdateAvailable(availableUpdate != null)
                        return@launch
                    }

                    try {
                        val update = withContext(Dispatchers.IO) {
                            UpdateCheck.fetch(
                                httpClient = sharedHttpClient,
                                manifestUrl = manifestUrl(),
                                currentVersionCode = BuildConfig.VERSION_CODE,
                                isDebugBuild = BuildConfig.DEBUG,
                            )
                        }
                        availableUpdate = update
                        checked = true
                        onUpdateAvailable(update != null)
                    } catch (e: Throwable) {
                        // A failed check is not cached, so the next map view
                        // retries instead of hiding the button for the session.
                        e.rethrowIfCancellation()
                    }
                }
            }
        }
    }

    /** Opens the update dialog, if a newer build was found. */
    fun showDialog() {
        val update = availableUpdate ?: return

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.update_available)
            .setMessage(
                if (isBeta) {
                    context.getString(
                        R.string.update_available_description_beta,
                        BuildConfig.VERSION_CODE, update.versionCode
                    )
                } else {
                    context.getString(
                        R.string.update_available_description,
                        BuildConfig.VERSION_NAME, update.versionName
                    )
                }
            )
            .setPositiveButton(R.string.get_apk) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = update.url.toUri()
                context.startActivity(intent)
            }
            .setNegativeButton(R.string.ignore, null)
            .show()
    }

    private companion object {
        val sharedHttpClient: HttpClient by lazy { apiHttpClient("btcmap-android") }

        /**
         * The check is identical for every map view in the process, so its
         * outcome is cached and reused. [checked] stays false until a check
         * succeeds, so a failure (for example while offline) is retried on the
         * next view instead of being remembered for the session.
         */
        @Volatile private var checked = false
        @Volatile private var availableUpdate: AvailableUpdate? = null

        val isBeta: Boolean
            get() = BuildConfig.BUILD_TYPE == "beta"

        fun manifestUrl(): String =
            if (isBeta) UpdateCheck.BETA_MANIFEST_URL else UpdateCheck.RELEASE_MANIFEST_URL
    }
}
