package org.btcmap.settings

import kotlinx.coroutines.runBlocking
import org.btcmap.db.Database
import org.btcmap.db.testDatabase
import org.btcmap.i18n.Strings
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Test

/**
 * The stored settings: their defaults, that a selection is written through to
 * the database, and the localized names the options render. Ported from the
 * Android instrumented suite, which was the only place covering this.
 */
class SettingsPersistenceTest {

    private fun settings(db: Database) =
        Settings(dbProvider = { db }, legacyValues = { emptyMap() })

    @Test
    fun mapStyle_defaultsToAuto_andPersistsSelection() = runBlocking<Unit> {
        val prefs = settings(testDatabase())
        Assert.assertEquals(MapStyle.Auto, prefs.mapStyle)

        prefs.mapStyle = MapStyle.Dark

        Assert.assertEquals(MapStyle.Dark, prefs.mapStyle)
    }

    @Test
    fun verifiedFilterYears_defaultsToThree_andPersistsSelection() = runBlocking<Unit> {
        val prefs = settings(testDatabase())
        Assert.assertEquals(3, prefs.verifiedFilterYears)

        prefs.verifiedFilterYears = 1

        Assert.assertEquals(1, prefs.verifiedFilterYears)
    }

    @Test
    fun apiUrl_defaultsToPublicApi_andPersistsOverride() = runBlocking<Unit> {
        val prefs = settings(testDatabase())
        Assert.assertEquals("https://api.btcmap.org", prefs.apiUrl.toString())

        prefs.apiUrl = "https://staging.example.com".toUrl()

        Assert.assertEquals("https://staging.example.com", prefs.apiUrl.toString())
    }

    @Test
    fun toggles_persistSelection() = runBlocking<Unit> {
        val prefs = settings(testDatabase())

        prefs.showAttribution = false
        prefs.mapRotationEnabled = true
        prefs.mapTiltEnabled = true

        Assert.assertFalse(prefs.showAttribution)
        Assert.assertTrue(prefs.mapRotationEnabled)
        Assert.assertTrue(prefs.mapTiltEnabled)
    }

    @Test
    fun mapStyle_rendersLocalizedName() {
        Assert.assertEquals(
            "OpenFreeMap Dark",
            Strings.forLocale("en")["style_dark"],
        )
    }

    @Test
    fun verifiedFilterYears_rendersLocalizedName() {
        Assert.assertEquals(
            "Verified within 2 years",
            Strings.forLocale("en")["verified_filter_2_years"],
        )
    }

    @Test
    fun activityInterval_rendersLocalizedName() {
        Assert.assertEquals(
            "1w",
            Strings.forLocale("en")["activity_interval_week"],
        )
    }
}
