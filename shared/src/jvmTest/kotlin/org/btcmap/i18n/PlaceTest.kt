package org.btcmap.i18n

import org.btcmap.db.table.place.Place
import org.btcmap.json.parseJsonObject
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.util.Locale

class PlaceTest {

    private fun place(
        name: String? = "Grand Paris",
        localizedName: String? = null,
    ) = Place(
        id = 1L,
        updatedAt = ZonedDateTime.parse("2026-01-01T00:00:00Z"),
        lat = 0.0,
        lon = 0.0,
        icon = "store",
        name = name,
        localizedName = localizedName?.let { parseJsonObject(it) },
        verifiedAt = null,
        address = null,
        openingHours = null,
        phone = null,
        website = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
        requiredAppUrl = null,
        boostedUntil = null,
        comments = null,
        telegram = null,
        osmId = null,
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
        val place = place(localizedName = """{"en":"Paris","ru":"Париж"}""")

        Assert.assertEquals("Париж", withLocale("ru") { place.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_fallsBackToTheSyncedName() {
        // The synced name is already English or the base tag, so the client
        // does not look up English itself.
        val place = place(name = "Paris", localizedName = """{"ru":"Париж"}""")

        Assert.assertEquals("Paris", withLocale("de") { place.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_ignoresBlankAndNonStringValues() {
        val place = place(
            name = "Paris",
            localizedName = """{"de":"","fr":123,"it":null,"es":{"nested":true}}""",
        )

        Assert.assertEquals("Paris", withLocale("de") { place.getLocalizedName() })
        Assert.assertEquals("Paris", withLocale("fr") { place.getLocalizedName() })
        Assert.assertEquals("Paris", withLocale("it") { place.getLocalizedName() })
        Assert.assertEquals("Paris", withLocale("es") { place.getLocalizedName() })
    }

    @Test
    fun getLocalizedName_isEmptyWhenNameMissing() {
        Assert.assertEquals("", withLocale("ru") { place(name = null).getLocalizedName() })
    }

    @Test
    fun getSearchableNames_includesSyncedNameAndTranslations() {
        val place = place(name = "Bakery", localizedName = """{"ru":"Пекарня","en":"Bakery"}""")

        Assert.assertEquals(
            setOf("Bakery", "Пекарня"),
            place.getSearchableNames().toSet(),
        )
    }

    @Test
    fun getSearchableNames_isEmptyWhenThePlaceHasNoName() {
        Assert.assertTrue(place(name = null).getSearchableNames().isEmpty())
    }
}
