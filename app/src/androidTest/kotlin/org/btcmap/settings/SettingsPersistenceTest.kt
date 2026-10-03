package org.btcmap.settings

import kotlinx.coroutines.runBlocking
import androidx.core.graphics.toColorInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.btcmap.util.toUrl
import org.btcmap.R
import org.btcmap.util.AppTestCase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest : AppTestCase() {

    private val prefs get() = preferencesRule.prefs

    @Test
    fun mapStyle_defaultsToAuto_andPersistsSelection() = runBlocking<Unit> {
        Assert.assertEquals(MapStyle.Auto, prefs.mapStyle)

        prefs.mapStyle = MapStyle.Dark

        Assert.assertEquals(MapStyle.Dark, prefs.mapStyle)
    }

    @Test
    fun verifiedFilterYears_defaultsToThree_andPersistsSelection() = runBlocking<Unit> {
        Assert.assertEquals(3, prefs.verifiedFilterYears)

        prefs.verifiedFilterYears = 1

        Assert.assertEquals(1, prefs.verifiedFilterYears)
    }

    @Test
    fun apiUrl_defaultsToPublicApi_andPersistsOverride() = runBlocking<Unit> {
        Assert.assertEquals("https://api.btcmap.org", prefs.apiUrl.toString())

        prefs.apiUrl = "https://staging.example.com".toUrl()

        Assert.assertEquals("https://staging.example.com", prefs.apiUrl.toString())
    }

    @Test
    fun toggles_persistSelection() = runBlocking<Unit> {
        prefs.showAttribution = false
        prefs.mapRotationEnabled = true

        Assert.assertFalse(prefs.showAttribution)
        Assert.assertTrue(prefs.mapRotationEnabled)
    }

    @Test
    fun authToken_persistsAndReflectsAuthorization() = runBlocking<Unit> {
        Assert.assertNull(prefs.authToken)
        Assert.assertFalse(prefs.authorized)

        prefs.setAuthTokenForTesting("secret-token")

        Assert.assertEquals("secret-token", prefs.authToken)
        Assert.assertTrue(prefs.authorized)

        prefs.setAuthTokenForTesting(null)

        Assert.assertFalse(prefs.authorized)
    }

    @Test
    fun customMarkerColor_persists_andResetsToDefault() = runBlocking<Unit> {
        val custom = "#123456".toColorInt()

        prefs.setMarkerBackgroundColor(custom)

        Assert.assertEquals(custom, prefs.markerBackgroundColor(preferencesRule.context))

        prefs.setMarkerBackgroundColor(null)

        Assert.assertEquals(
            0xFF0e95af.toInt(),
            prefs.markerBackgroundColor(preferencesRule.context),
        )
    }

    @Test
    fun mapStyle_rendersLocalizedResourceName() = runBlocking<Unit> {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.style_dark),
            MapStyle.Dark.name(preferencesRule.context),
        )
    }

    @Test
    fun verifiedFilterYears_rendersLocalizedResourceName() = runBlocking<Unit> {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.verified_filter_2_years),
            2.toVerifiedFilterYears(preferencesRule.context),
        )
    }

    @Test
    fun activityInterval_rendersLocalizedResourceName() = runBlocking<Unit> {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.activity_interval_week),
            ActivityInterval.Week.name(preferencesRule.context),
        )
    }
}
