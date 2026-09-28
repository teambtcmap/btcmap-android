package org.btcmap.map

import org.junit.Assert
import org.junit.Test

class UpdateNotificationTest {

    @Test
    fun newerPublishedVersion_isAvailable() {
        Assert.assertTrue(
            isUpdateAvailable(currentVersionCode = 5, latestVersionCode = 6, isDebugBuild = false),
        )
    }

    @Test
    fun sameOrOlderPublishedVersion_isNotAvailable() {
        Assert.assertFalse(
            isUpdateAvailable(currentVersionCode = 6, latestVersionCode = 6, isDebugBuild = false),
        )
        Assert.assertFalse(
            isUpdateAvailable(currentVersionCode = 6, latestVersionCode = 5, isDebugBuild = false),
        )
    }

    @Test
    fun debugBuild_neverOffersAnUpdate() {
        // Debug builds are installed locally and must not be nagged about the
        // published APK, even when its version code is far ahead.
        Assert.assertFalse(
            isUpdateAvailable(currentVersionCode = 1, latestVersionCode = 999, isDebugBuild = true),
        )
    }
}
