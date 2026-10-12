package org.btcmap.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.BuildConfig
import org.btcmap.api.apiHttpClient
import org.btcmap.i18n.Strings
import org.btcmap.update.AvailableUpdate
import org.btcmap.update.UpdateCheck
import org.btcmap.util.rethrowIfCancellation
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reports whether the published APK is newer than this build, and opens the
 * update dialog when the map's update button is tapped.
 *
 * The check is shared [UpdateCheck] logic; this only supplies the app's build
 * numbers and manifest URL, hands the button's visibility to the shared map
 * through [onUpdateAvailable], and shows the dialog. The check is process-scoped:
 * its outcome is cached and reused, so a rotation or a return to the map does
 * not re-issue it. The HTTP client is shared for the whole process.
 *
 * The beta build installs its own updates: tapping the dialog's action downloads
 * the APK with a progress bar and hands it to the system package installer,
 * asking the user to allow installs from this app first when the OS requires it.
 * The release build cannot self-install, so its action still opens the APK's URL
 * in the browser.
 */
class UpdateNotificationController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onUpdateAvailable: (Boolean) -> Unit,
) {
    /** A "what am I running" label for the settings screen. */
    val versionLabel: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

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
                        val update = withContext(Dispatchers.IO) { fetch() }
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

    /**
     * Re-checks the channel, the settings row's manual action. A newer build
     * opens the update dialog; otherwise [onResult] is shown with the outcome.
     */
    fun checkForUpdates(onResult: (String) -> Unit) {
        val strings = Strings.current()
        lifecycleOwner.lifecycleScope.launch {
            try {
                val update = withContext(Dispatchers.IO) { fetch() }
                availableUpdate = update
                checked = true
                onUpdateAvailable(update != null)
                if (update != null) {
                    showDialog()
                } else {
                    onResult(strings["you_are_up_to_date"])
                }
            } catch (e: Throwable) {
                e.rethrowIfCancellation()
                onResult(strings["could_not_check_for_updates"])
            }
        }
    }

    /** Opens the update dialog, if a newer build was found. */
    fun showDialog() {
        val update = availableUpdate ?: return
        val strings = Strings.current()

        MaterialAlertDialogBuilder(context)
            .setTitle(strings["update_available"])
            .setMessage(
                if (isBeta) {
                    strings.format(
                        "update_available_description_beta",
                        BuildConfig.VERSION_CODE, update.versionCode
                    )
                } else {
                    strings.format(
                        "update_available_description",
                        BuildConfig.VERSION_NAME, update.versionName
                    )
                }
            )
            .setPositiveButton(if (isBeta) strings["update"] else strings["get_apk"]) { _, _ ->
                if (isBeta) installUpdate(update) else openInBrowser(update)
            }
            .setNegativeButton(strings["ignore"], null)
            .show()
    }

    /** Opens the APK's URL in the browser; the release build cannot self-install. */
    private fun openInBrowser(update: AvailableUpdate) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = update.url.toUri()
        context.startActivity(intent)
    }

    /**
     * The beta build's self-update: asks for the install permission if needed,
     * then downloads the APK and hands it to the system installer.
     */
    private fun installUpdate(update: AvailableUpdate) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            // Ask for the permission before spending a download on it.
            openInstallPermissionSettings()
            showMessage(Strings.current()["allow_install_updates"])
            return
        }

        val strings = Strings.current()
        val progress = showProgressDialog(strings["downloading"])

        lifecycleOwner.lifecycleScope.launch {
            val apk = downloadApk(update.url) { fraction ->
                progress.update(fraction)
            }
            progress.dismiss()
            if (apk == null) {
                showMessage(strings["could_not_download_update"])
                return@launch
            }
            if (!launchInstaller(apk)) {
                showMessage(strings["could_not_install_update"])
            }
        }
    }

    /**
     * Downloads [url] into the cache, reporting progress in `0f..1f` (null while
     * the size is unknown). Returns the file, or null on any failure.
     */
    private suspend fun downloadApk(url: String, onProgress: (Double?) -> Unit): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
                val file = File(dir, UPDATE_APK_NAME)
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    val total = connection.contentLengthLong.takeIf { it > 0 }
                    connection.inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                read += count
                                onProgress(total?.let { read.toDouble() / it })
                            }
                        }
                    }
                    file
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    /** Hands [apk] to the system package installer; false when it cannot start. */
    private fun launchInstaller(apk: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess

    /** Opens the per-app "install unknown apps" settings screen. */
    private fun openInstallPermissionSettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * A modal progress dialog over the download. The returned handle is fed a
     * `0f..1f` fraction from the download thread; a null fraction — the size is
     * unknown — leaves the bar indeterminate.
     */
    private fun showProgressDialog(title: String): ProgressDialogHandle {
        val density = context.resources.displayMetrics.density
        val padding = (24 * density).toInt()
        val indicator = LinearProgressIndicator(context).apply { isIndeterminate = true }
        val container = FrameLayout(context).apply {
            setPadding(padding, padding / 2, padding, padding / 2)
            addView(
                indicator,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setCancelable(false)
            .create()
        dialog.show()

        val mainHandler = Handler(Looper.getMainLooper())
        return ProgressDialogHandle(dialog) { fraction ->
            mainHandler.post {
                if (fraction == null) {
                    indicator.isIndeterminate = true
                } else {
                    indicator.isIndeterminate = false
                    indicator.setProgressCompat((fraction * 100).toInt(), false)
                }
            }
        }
    }

    /** The running progress dialog and a way to feed it a download fraction. */
    private class ProgressDialogHandle(
        private val dialog: android.app.Dialog,
        private val onFraction: (Double?) -> Unit,
    ) {
        fun update(fraction: Double?) = onFraction.invoke(fraction)

        fun dismiss() {
            runCatching { dialog.dismiss() }
        }
    }

    private suspend fun fetch(): AvailableUpdate? = UpdateCheck.fetch(
        httpClient = sharedHttpClient,
        manifestUrl = manifestUrl(),
        currentVersionCode = BuildConfig.VERSION_CODE,
        isDebugBuild = BuildConfig.DEBUG,
        currentBuildTime = BuildConfig.BUILD_TIME,
    )

    private companion object {
        const val UPDATES_DIR = "updates"
        const val UPDATE_APK_NAME = "btcmap-update.apk"
        const val USER_AGENT = "btcmap-android"

        val sharedHttpClient: HttpClient by lazy { apiHttpClient(USER_AGENT) }

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
