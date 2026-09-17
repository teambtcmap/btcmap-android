package org.btcmap.settings

import androidx.core.graphics.toColorInt
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.R
import org.btcmap.util.DatabaseRule
import org.btcmap.util.PreferencesRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @JvmField
    @Rule
    val databaseRule = DatabaseRule()

    @JvmField
    @Rule
    val preferencesRule = PreferencesRule()

    private val prefs get() = preferencesRule.prefs

    @Test
    fun mapStyle_defaultsToAuto_andPersistsSelection() {
        Assert.assertEquals(MapStyle.Auto, prefs.mapStyle)

        prefs.mapStyle = MapStyle.Dark

        Assert.assertEquals(MapStyle.Dark, prefs.mapStyle)
    }

    @Test
    fun verifiedFilterYears_defaultsToThree_andPersistsSelection() {
        Assert.assertEquals(3, prefs.verifiedFilterYears)

        prefs.verifiedFilterYears = 1

        Assert.assertEquals(1, prefs.verifiedFilterYears)
    }

    @Test
    fun apiUrl_defaultsToPublicApi_andPersistsOverride() {
        Assert.assertEquals("https://api.btcmap.org/", prefs.apiUrl.toString())

        prefs.apiUrl = "https://staging.example.com".toHttpUrl()

        Assert.assertEquals("https://staging.example.com/", prefs.apiUrl.toString())
    }

    @Test
    fun toggles_persistSelection() {
        Assert.assertFalse(prefs.useAdaptiveColors)

        prefs.useAdaptiveColors = true
        prefs.showAttribution = false
        prefs.mapRotationEnabled = true

        Assert.assertTrue(prefs.useAdaptiveColors)
        Assert.assertFalse(prefs.showAttribution)
        Assert.assertTrue(prefs.mapRotationEnabled)
    }

    @Test
    fun authToken_persistsAndReflectsAuthorization() {
        Assert.assertNull(prefs.authToken)
        Assert.assertFalse(prefs.authorized)

        prefs.authToken = "secret-token"

        Assert.assertEquals("secret-token", prefs.authToken)
        Assert.assertTrue(prefs.authorized)

        prefs.authToken = null

        Assert.assertFalse(prefs.authorized)
    }

    @Test
    fun customMarkerColor_persists_andResetsToDefault() {
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
    fun mapStyle_rendersLocalizedResourceName() {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.style_carto_dark_matter),
            MapStyle.CartoDarkMatter.name(preferencesRule.context),
        )
    }

    @Test
    fun verifiedFilterYears_rendersLocalizedResourceName() {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.verified_filter_2_years),
            2.toVerifiedFilterYears(preferencesRule.context),
        )
    }

    @Test
    fun activityInterval_rendersLocalizedResourceName() {
        Assert.assertEquals(
            preferencesRule.context.getString(R.string.activity_interval_week),
            ActivityInterval.Week.name(preferencesRule.context),
        )
    }
}
