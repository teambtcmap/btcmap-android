package org.btcmap.saved

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.user.SavedItem
import org.btcmap.json.parseJsonObject
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant
import java.util.Locale

class SavedItemNamesTest {

    private fun database() = Database(BundledSQLiteDriver(), ":memory:")

    private fun place(id: Long, name: String?, localizedName: String?) = Place(
        id = id,
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
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

    private fun area(id: Long, name: String, localizedName: String?) = Area(
        id = id,
        name = name,
        type = "community",
        urlAlias = "area-$id",
        icon = null,
        iconWide = null,
        websiteUrl = "https://btcmap.org/community/$id",
        description = null,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = null,
        localizedName = localizedName?.let { parseJsonObject(it) },
        localizedDescription = null,
    )

    private suspend fun <T> withLocale(language: String, block: suspend () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(language))
        try {
            return block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun withLocalizedPlaceNames_usesTheLocalizedCachedName() = runTest {
        val db = database()
        db.place.insert(listOf(place(id = 1, name = "Paris", localizedName = """{"ru":"Париж"}""")))

        val result = withLocale("ru") {
            listOf(SavedItem(id = 1, name = "Paris")).withLocalizedPlaceNames(db)
        }

        Assert.assertEquals("Париж", result.single().name)
    }

    @Test
    fun withLocalizedPlaceNames_fallsBackToTheServerName() = runTest {
        val db = database()

        // No cached place row, so the server's name is kept.
        val result = listOf(SavedItem(id = 1, name = "Paris")).withLocalizedPlaceNames(db)

        Assert.assertEquals("Paris", result.single().name)
    }

    @Test
    fun withLocalizedAreaNames_usesTheLocalizedCachedName() = runTest {
        val db = database()
        db.area.insert(listOf(area(id = 2, name = "Grand Paris", localizedName = """{"ru":"Большой Париж"}""")))

        val result = withLocale("ru") {
            listOf(SavedItem(id = 2, name = "Grand Paris")).withLocalizedAreaNames(db)
        }

        Assert.assertEquals("Большой Париж", result.single().name)
    }
}
