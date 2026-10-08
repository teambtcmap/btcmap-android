package org.btcmap.settings

import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.btcmap.db.testDatabase
import org.btcmap.i18n.Strings
import org.btcmap.platform.currentLanguage
import org.btcmap.platform.currentLocale
import org.btcmap.platform.languageOverride
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The stored UI language and the platform override it drives: choosing a
 * language makes both the locale and the string catalog resolve to it, and
 * clearing it follows the device again.
 */
class LanguageSettingsTest {

    @After
    fun resetOverride() {
        // The override is process-wide, so a test must not leak it.
        languageOverride = null
    }

    @Test
    fun language_defaultsToFollowingTheDevice() {
        val settings = settings()

        assertNull(settings.language)
    }

    @Test
    fun applyLanguage_overridesTheLocaleAndTheCatalog() {
        val settings = settings()
        settings.language = "de"
        settings.applyLanguage()

        assertEquals("de", currentLocale())
        assertEquals("de", currentLanguage())
        // A key the German table translates, proving it, not English, is used.
        assertEquals("Kartenstil", Strings.current()["map_style"])
    }

    @Test
    fun applyLanguage_keepsARegionalVariant() {
        val settings = settings()
        settings.language = "pt-BR"
        settings.applyLanguage()

        assertEquals("pt-BR", currentLocale())
        // Content localization uses the base language.
        assertEquals("pt", currentLanguage())
    }

    @Test
    fun clearingTheLanguage_followsTheDeviceAgain() {
        val settings = settings()
        settings.language = "de"
        settings.applyLanguage()

        settings.language = null
        settings.applyLanguage()

        assertNull(languageOverride)
        assertEquals(Locale.getDefault().toLanguageTag(), currentLocale())
    }

    private fun settings(): Settings {
        val db = testDatabase()
        return runBlocking {
            Settings(dbProvider = { db }, legacyValues = { emptyMap() }).apply { preload() }
        }
    }
}
