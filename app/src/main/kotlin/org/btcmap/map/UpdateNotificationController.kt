package org.btcmap.map

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.util.rethrowIfCancellation

/**
 * Reports whether the published APK is newer than this build, and opens the
 * update dialog when the map's update button is tapped.
 *
 * The check is independent of the map, so the controller hands the button's
 * visibility to the shared map through [onUpdateAvailable] rather than touching
 * a view itself. The check is process-scoped: its outcome is cached and reused,
 * so a rotation or a return to the map does not re-issue it. The HTTP client is
 * shared for the whole process.
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
                        val update = withContext(Dispatchers.IO) { fetchLatestUpdate() }
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
        val sharedHttpClient: OkHttpClient by lazy { OkHttpClient() }

        /**
         * The check is identical for every map view in the process, so its
         * outcome is cached and reused. [checked] stays false until a check
         * succeeds, so a failure (for example while offline) is retried on the
         * next view instead of being remembered for the session.
         */
        @Volatile private var checked = false
        @Volatile private var availableUpdate: AvailableUpdate? = null

        const val RELEASE_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-ver.json"
        const val BETA_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-beta-ver.json"

        val isBeta: Boolean
            get() = BuildConfig.BUILD_TYPE == "beta"

        fun manifestUrl(): String = if (isBeta) BETA_MANIFEST_URL else RELEASE_MANIFEST_URL

        suspend fun fetchLatestUpdate(): AvailableUpdate? {
            val latestVerJson = sharedHttpClient.newCall(
                Request.Builder()
                    .url(manifestUrl().toHttpUrl())
                    .build()
            ).executeAsync().use { it.body.string().trim() }

            val latestVer = JsonParser.parseString(latestVerJson).asJsonObject
            val latestVerCode = latestVer.get("code").asInt
            val latestVerName = latestVer.get("name").asString
            val latestVerUrl = latestVer.get("url").asString

            if (!isUpdateAvailable(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    latestVersionCode = latestVerCode,
                    isDebugBuild = BuildConfig.DEBUG,
                )
            ) {
                return null
            }

            return AvailableUpdate(latestVerCode, latestVerName, latestVerUrl)
        }
    }
}

private class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val url: String,
)

/**
 * Whether the published APK is newer than this build. Debug builds are installed
 * locally and must not be nagged about the published APK, even when their
 * version code is lower.
 */
internal fun isUpdateAvailable(
    currentVersionCode: Int,
    latestVersionCode: Int,
    isDebugBuild: Boolean,
): Boolean = !isDebugBuild && latestVersionCode > currentVersionCode
