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
    /**
     * The download's SHA-256, when the manifest names one. The Linux updater
     * verifies it before replacing the running AppImage; the Android installer
     * checks the APK's own signature instead.
     */
    val sha256: String? = null,
)

/**
 * Checks a published build manifest against the running build, so Android and
 * the desktop share one definition of the update JSON and when it counts as
 * newer.
 *
 * The Android manifests are a single entry. The desktop's `metadata.json` is a
 * dictionary of entries keyed by build id, one per platform and channel, which
 * lets new platforms join without changing the file's shape.
 */
object UpdateCheck {

    const val RELEASE_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-ver.json"

    const val BETA_MANIFEST_URL = "https://static.btcmap.org/android/latest-app-beta-ver.json"

    const val DESKTOP_BETA_MANIFEST_URL = "https://static.btcmap.org/builds/metadata.json"

    /**
     * The metadata entry this AppImage follows. The metadata is a dictionary
     * keyed by build id so the same file can carry every platform, package
     * format and channel later (`appimage-beta`, `android-release`,
     * `windows-beta`, …).
     */
    const val APPIMAGE_BETA_ID = "appimage-beta"

    /**
     * Reads [manifestUrl] and returns the update it names, or null when this
     * build is current. [id] selects an entry from a dictionary manifest; when
     * the manifest is a single entry, as the Android ones are, it is read
     * directly. Debug builds are installed locally and never nagged, even when
     * their version code is lower.
     */
    suspend fun fetch(
        httpClient: HttpClient,
        manifestUrl: String,
        currentVersionCode: Int,
        isDebugBuild: Boolean,
        currentBuildTime: Long = 0L,
        id: String? = null,
    ): AvailableUpdate? {
        val json = httpClient.executeIdempotent(HttpMethod.Get, manifestUrl.toUrl())
            .bodyAsText().trim()

        val root = parseJson(json).jsonObject
        // A missing id falls back to the root so a manifest published before
        // the dictionary shape (or the Android ones) keeps working.
        val latest = id?.let { key -> root[key]?.jsonObject }
            ?: root.takeIf { it["code"] != null }
            ?: error("update manifest has no entry '$id'")
        val latestVersionCode = latest["code"]!!.jsonPrimitive.int
        val latestVersionName = latest["name"]!!.jsonPrimitive.content
        val url = latest["url"]!!.jsonPrimitive.content
        // The release manifest has no build time; only the beta deploy stamps
        // one. A missing field reads as zero, so a release check keeps comparing
        // version codes alone.
        val latestBuildTime = latest["buildTime"]?.jsonPrimitive?.longOrNull ?: 0L
        val sha256 = latest["sha256"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

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
            sha256 = sha256,
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
