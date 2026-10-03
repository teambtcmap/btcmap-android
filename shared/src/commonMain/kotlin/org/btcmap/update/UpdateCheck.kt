package org.btcmap.update

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.http.executeIdempotent
import org.btcmap.json.parseJson
import org.btcmap.util.toUrl

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
        httpClient: HttpClient,
        manifestUrl: String,
        currentVersionCode: Int,
        isDebugBuild: Boolean,
    ): AvailableUpdate? {
        val json = httpClient.executeIdempotent(HttpMethod.Get, manifestUrl.toUrl())
            .bodyAsText().trim()

        val latest = parseJson(json).jsonObject
        val latestVersionCode = latest["code"]!!.jsonPrimitive.int
        val latestVersionName = latest["name"]!!.jsonPrimitive.content
        val url = latest["url"]!!.jsonPrimitive.content

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
