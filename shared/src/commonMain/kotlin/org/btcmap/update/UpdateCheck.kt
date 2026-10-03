package org.btcmap.update

import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync

/** A published build newer than the one running. */
data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val url: String,
)

/**
 * Checks the published APK manifest against the running build, so Android and
 * the desktop share one definition of the update JSON and when it counts as
 * newer.
 */
object UpdateCheck {

    const val RELEASE_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-ver.json"

    const val BETA_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-beta-ver.json"

    /**
     * Reads [manifestUrl] and returns the update it names, or null when this
     * build is current. Debug builds are installed locally and never nagged,
     * even when their version code is lower.
     */
    suspend fun fetch(
        httpClient: OkHttpClient,
        manifestUrl: String,
        currentVersionCode: Int,
        isDebugBuild: Boolean,
    ): AvailableUpdate? {
        val json = httpClient.newCall(Request.Builder().url(manifestUrl.toHttpUrl()).build())
            .executeAsync().use { it.body.string().trim() }

        val latest = JsonParser.parseString(json).asJsonObject
        val latestVersionCode = latest.get("code").asInt
        val latestVersionName = latest.get("name").asString
        val url = latest.get("url").asString

        if (!isUpdateAvailable(currentVersionCode, latestVersionCode, isDebugBuild)) return null

        return AvailableUpdate(
            versionCode = latestVersionCode,
            versionName = latestVersionName,
            url = url,
        )
    }

    /**
     * Whether the published APK is newer than this build. Debug builds are
     * installed locally and must not be nagged about the published APK, even
     * when their version code is lower.
     */
    fun isUpdateAvailable(
        currentVersionCode: Int,
        latestVersionCode: Int,
        isDebugBuild: Boolean,
    ): Boolean = !isDebugBuild && latestVersionCode > currentVersionCode
}
