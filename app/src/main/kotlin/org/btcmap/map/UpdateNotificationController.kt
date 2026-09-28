package org.btcmap.map

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.view.IconButton
import org.btcmap.util.rethrowIfCancellation

/**
 * Shows an update button when the published APK is newer than this build.
 *
 * The check and the button's tap are independent of the map, but the button
 * lives on the map, so the controller is rebuilt with every map view. It must
 * therefore not own anything expensive: the HTTP client is shared for the whole
 * process instead of being created (and leaked) per instance.
 */
class UpdateNotificationController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val icon: IconButton,
) {
    init {
        icon.isVisible = false
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.withResumed {
                launch {
                    try {
                        val latestVerJson = withContext(Dispatchers.IO) {
                            sharedHttpClient.newCall(
                                Request.Builder()
                                    .url(manifestUrl().toHttpUrl())
                                    .build()
                            ).executeAsync().use { it.body.string().trim() }
                        }

                        val latestVer =
                            com.google.gson.JsonParser.parseString(latestVerJson).asJsonObject
                        val latestVerCode = latestVer.get("code").asInt
                        val latestVerName = latestVer.get("name").asString
                        val latestVerUrl = latestVer.get("url").asString

                        if (isUpdateAvailable(
                                currentVersionCode = BuildConfig.VERSION_CODE,
                                latestVersionCode = latestVerCode,
                                isDebugBuild = BuildConfig.DEBUG,
                            )
                        ) {
                            icon.isVisible = true
                            icon.iconColor(context.getErrorColor())

                            icon.setOnClickListener {
                                MaterialAlertDialogBuilder(context)
                                    .setTitle(R.string.update_available)
                                    .setMessage(
                                        if (isBeta) {
                                            context.getString(
                                                R.string.update_available_description_beta,
                                                BuildConfig.VERSION_CODE, latestVerCode
                                            )
                                        } else {
                                            context.getString(
                                                R.string.update_available_description,
                                                BuildConfig.VERSION_NAME, latestVerName
                                            )
                                        }
                                    )
                                    .setPositiveButton(R.string.get_apk) { _, _ ->
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = latestVerUrl.toUri()
                                        context.startActivity(intent)
                                    }
                                    .setNegativeButton(R.string.ignore, null)
                                    .show()
                            }
                        }
                    } catch (e: Throwable) {
                        e.rethrowIfCancellation()
                    }
                }
            }
        }
    }

    private val isBeta: Boolean
        get() = BuildConfig.BUILD_TYPE == "beta"

    private fun manifestUrl(): String {
        return if (isBeta) {
            BETA_MANIFEST_URL
        } else {
            RELEASE_MANIFEST_URL
        }
    }

    private companion object {
        val sharedHttpClient: OkHttpClient by lazy { OkHttpClient() }

        const val RELEASE_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-ver.json"
        const val BETA_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-beta-ver.json"
    }
}

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
