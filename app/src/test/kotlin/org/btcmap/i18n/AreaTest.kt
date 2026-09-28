package org.btcmap.i18n

import com.google.gson.JsonParser
import org.btcmap.db.table.area.Area
import org.junit.Assert
import org.junit.Test
import java.util.Locale

class AreaTest {

    private fun area(
        name: String = "Grand Paris",
        description: String? = "Greater Paris",
        localizedName: String? = null,
        localizedDescription: String? = null,
    ) = Area(
        id = 1L,
        name = name,
        type = "community",
        urlAlias = "grand-paris",
        icon = null,
        iconWide = null,
        websiteUrl = "https://btcmap.org/community/grand-paris",
        description = description,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = null,
        localizedName = localizedName?.let { JsonParser.parseString(it).asJsonObject },
        localizedDescription = localizedDescription?.let { JsonParser.parseString(it).asJsonObject },
    )

    private fun <T> withLocale(language: String, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(language))
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun getLocalizedName_prefersCurrentLanguage() {
        val area = area(localizedName = """{"en":"Grand Paris","ru":"Большой Париж"}""")

        Assert.assertEquals("Большой Париж", withLocale("ru") { area.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_fallsBackToEnglish() {
        val area = area(localizedName = """{"en":"Grand Paris","ru":"Большой Париж"}""")

        Assert.assertEquals("Grand Paris", withLocale("de") { area.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_fallsBackToBaseName() {
        val area = area(localizedName = """{"ru":"Большой Париж"}""")

        Assert.assertEquals("Grand Paris", withLocale("de") { area.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_usesBaseNameWhenNoTranslations() {
        Assert.assertEquals("Grand Paris", withLocale("ru") { area().getLocalizedName() })
    }

    @Test
    fun getLocalizedName_ignoresNonStringValues() {
        val area = area(localizedName = """{"ru":null,"en":123,"de":"Deutsch"}""")

        Assert.assertEquals("Deutsch", withLocale("de") { area.getLocalizedName() })
        Assert.assertEquals("Grand Paris", withLocale("fr") { area.getLocalizedName() })
    }

    @Test
    fun getLocalizedDescription_prefersCurrentLanguageThenEnglishThenBase() {
        val area = area(
            localizedDescription = """{"ru":"Большой Париж","en":"Greater Paris"}""",
        )
        val russianOnly = area(localizedDescription = """{"ru":"Большой Париж"}""")

        Assert.assertEquals(
            "Большой Париж",
            withLocale("ru") { area.getLocalizedDescription() },
        )
        Assert.assertEquals(
            "Greater Paris",
            withLocale("de") { area.getLocalizedDescription() },
        )
        Assert.assertEquals(
            "Greater Paris",
            withLocale("fr") { russianOnly.getLocalizedDescription() },
        )
    }

    @Test
    fun getLocalizedDescription_isNullWhenMissing() {
        Assert.assertNull(withLocale("ru") { area(description = null).getLocalizedDescription() })
    }
}
