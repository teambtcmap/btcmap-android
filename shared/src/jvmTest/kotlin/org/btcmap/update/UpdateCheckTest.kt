package org.btcmap.update

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.test.runTest
import org.btcmap.api.ApiTestBase
import org.junit.Assert
import org.junit.Test

class UpdateCheckTest : ApiTestBase() {

    @Test
    fun newerPublishedVersion_isAvailable() {
        Assert.assertTrue(
            UpdateCheck.isUpdateAvailable(
                currentVersionCode = 5,
                latestVersionCode = 6,
                isDebugBuild = false,
            ),
        )
    }

    @Test
    fun sameOrOlderPublishedVersion_isNotAvailable() {
        Assert.assertFalse(
            UpdateCheck.isUpdateAvailable(currentVersionCode = 6, latestVersionCode = 6, isDebugBuild = false),
        )
        Assert.assertFalse(
            UpdateCheck.isUpdateAvailable(currentVersionCode = 6, latestVersionCode = 5, isDebugBuild = false),
        )
    }

    @Test
    fun debugBuild_neverOffersAnUpdate() {
        // Debug builds are installed locally and must not be nagged about the
        // published APK, even when its version code is far ahead.
        Assert.assertFalse(
            UpdateCheck.isUpdateAvailable(currentVersionCode = 1, latestVersionCode = 999, isDebugBuild = true),
        )
    }

    @Test
    fun laterBuildAtTheSameVersionCode_isAvailable() {
        // A beta is rebuilt at one version code, so the deploy's build time is
        // what makes the next rollout an update.
        Assert.assertTrue(
            UpdateCheck.isUpdateAvailable(
                currentVersionCode = 6,
                latestVersionCode = 6,
                currentBuildTime = 1_000L,
                latestBuildTime = 2_000L,
                isDebugBuild = false,
            ),
        )
        Assert.assertFalse(
            UpdateCheck.isUpdateAvailable(
                currentVersionCode = 6,
                latestVersionCode = 6,
                currentBuildTime = 2_000L,
                latestBuildTime = 1_000L,
                isDebugBuild = false,
            ),
        )
    }

    @Test
    fun missingBuildTimes_neverCountAsNewer() {
        // The release manifest carries no build time, so an unchanged version
        // code stays current.
        Assert.assertFalse(
            UpdateCheck.isUpdateAvailable(
                currentVersionCode = 6,
                latestVersionCode = 6,
                isDebugBuild = false,
            ),
        )
    }

    @Test
    fun fetch_readsTheBuildTime() = runTest {
        enqueueJson(
            """{"code": 240, "name": "1.3.0", "buildTime": 1728600000000, "url": "https://example.com/app.apk"}""",
        )

        val update = UpdateCheck.fetch(
            httpClient = HttpClient(CIO),
            manifestUrl = server.url("/latest-app-beta-ver.json").toString(),
            currentVersionCode = 240,
            isDebugBuild = false,
            currentBuildTime = 1728500000000L,
        )

        Assert.assertEquals(1728600000000L, update?.buildTime)
    }

    @Test
    fun fetch_returnsTheNewerBuild() = runTest {
        enqueueJson("""{"code": 240, "name": "1.3.0", "url": "https://example.com/app.apk"}""")

        val update = UpdateCheck.fetch(
            httpClient = HttpClient(CIO),
            manifestUrl = server.url("/latest-app-ver.json").toString(),
            currentVersionCode = 239,
            isDebugBuild = false,
        )

        Assert.assertEquals(240, update?.versionCode)
        Assert.assertEquals("1.3.0", update?.versionName)
        Assert.assertEquals("https://example.com/app.apk", update?.url)
    }

    @Test
    fun fetch_returnsNullWhenThisBuildIsCurrent() = runTest {
        enqueueJson("""{"code": 239, "name": "1.2.0", "url": "https://example.com/app.apk"}""")

        val update = UpdateCheck.fetch(
            httpClient = HttpClient(CIO),
            manifestUrl = server.url("/latest-app-ver.json").toString(),
            currentVersionCode = 239,
            isDebugBuild = false,
        )

        Assert.assertNull(update)
    }
}
