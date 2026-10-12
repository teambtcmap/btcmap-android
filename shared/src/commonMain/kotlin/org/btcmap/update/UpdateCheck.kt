package org.btcmap.update

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.btcmap.http.executeIdempotent
import org.btcmap.json.parseJson
import org.btcmap.util.toUrl

/** A published build newer than the one running. */
data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    /**
     * When the published build was made, in epoch milliseconds. The
     * tie-breaker within one version code: a beta is rebuilt many times at the
     * same code, so [versionCode] alone cannot tell one deploy from the next.
     * Zero when the manifest carries none, which never looks newer than a
     * build stamped by a deploy.
     */
    val buildTime: Long = 0L,
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
        currentBuildTime: Long = 0L,
    ): AvailableUpdate? {
        val json = httpClient.executeIdempotent(HttpMethod.Get, manifestUrl.toUrl())
            .bodyAsText().trim()

        val latest = parseJson(json).jsonObject
        val latestVersionCode = latest["code"]!!.jsonPrimitive.int
        val latestVersionName = latest["name"]!!.jsonPrimitive.content
        val url = latest["url"]!!.jsonPrimitive.content
        // The release manifest has no build time; only the beta deploy stamps
        // one. A missing field reads as zero, so a release check keeps comparing
        // version codes alone.
        val latestBuildTime = latest["buildTime"]?.jsonPrimitive?.longOrNull ?: 0L

        if (
            !isUpdateAvailable(
                currentVersionCode = currentVersionCode,
                currentBuildTime = currentBuildTime,
                latestVersionCode = latestVersionCode,
                latestBuildTime = latestBuildTime,
                isDebugBuild = isDebugBuild,
            )
        ) {
            return null
        }

        return AvailableUpdate(
            versionCode = latestVersionCode,
            versionName = latestVersionName,
            url = url,
            buildTime = latestBuildTime,
        )
    }

    /**
     * Whether the published APK is newer than this build. Debug builds are
     * installed locally and must not be nagged about the published APK, even
     * when their version code is lower.
     *
     * A higher version code always wins. Within one version code, a later
     * [latestBuildTime] does: a beta stays at the release's code across
     * rebuilds, so the build time is what lets one deploy notice the next.
     */
    fun isUpdateAvailable(
        currentVersionCode: Int,
        latestVersionCode: Int,
        isDebugBuild: Boolean,
        currentBuildTime: Long = 0L,
        latestBuildTime: Long = 0L,
    ): Boolean {
        if (isDebugBuild) return false
        if (latestVersionCode != currentVersionCode) return latestVersionCode > currentVersionCode
        return latestBuildTime > currentBuildTime
    }
}
