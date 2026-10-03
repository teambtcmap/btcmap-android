package org.btcmap.settings

import kotlin.time.Clock
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class SettingsValueTest {

    private fun createDatabase() = Database(BundledSQLiteDriver(), ":memory:")

    private fun createSettings(db: Database) = Settings(
        dbProvider = { db },
        legacyValues = { emptyMap() },
    )

    @Test
    fun getIntOrNull_returnsNullWhenAbsent() {
        val settings = createSettings(createDatabase())

        Assert.assertNull(settings.getIntOrNull("markerBackgroundColor"))
    }

    @Test
    fun getIntOrNull_returnsStoredMinusOne() {
        // A custom opaque white is 0xFFFFFFFF, i.e. -1. Using -1 as the "no
        // override" sentinel would mistake it for an absent value.
        val settings = createSettings(createDatabase())

        settings.putInt("markerBackgroundColor", -1)

        Assert.assertEquals(-1, settings.getIntOrNull("markerBackgroundColor"))
    }

    @Test
    fun getIntOrNull_returnsNullForUnparseableValue() {
        val db = createDatabase()
        db.preference.upsert("markerBackgroundColor", "not a number")

        Assert.assertNull(createSettings(db).getIntOrNull("markerBackgroundColor"))
    }

    @Test
    fun putIntNullRemovesValue() {
        val settings = createSettings(createDatabase())

        settings.putInt("markerBackgroundColor", -1)
        settings.putInt("markerBackgroundColor", null)

        Assert.assertNull(settings.getIntOrNull("markerBackgroundColor"))
    }

    @Test
    fun verifiedFilterMinVerifiedAt_subtractsConfiguredYears() {
        val settings = createSettings(createDatabase())
        settings.verifiedFilterYears = 1
        val now = Instant.parse("2026-06-15T12:00:00Z")

        Assert.assertEquals(
            Instant.parse("2025-06-15T12:00:00Z"),
            settings.verifiedFilterMinVerifiedAt(now),
        )
    }

    @Test
    fun verifiedFilterMinVerifiedAt_defaultsToThreeYears() {
        val settings = createSettings(createDatabase())
        val now = Instant.parse("2026-06-15T12:00:00Z")

        Assert.assertEquals(
            Instant.parse("2023-06-15T12:00:00Z"),
            settings.verifiedFilterMinVerifiedAt(now),
        )
    }

    @Test
    fun verifiedFilterMinVerifiedAt_normalisesCutoffToUtc() {
        // A device east of UTC parses to a value whose offset is not UTC; the
        // cutoff must still be a bare UTC instant, because SQLite's julianday()
        // is compared against it in the viewport query.
        val settings = createSettings(createDatabase())
        settings.verifiedFilterYears = 1
        val now = Instant.parse("2026-06-15T12:00:00+07:00")

        val cutoff = settings.verifiedFilterMinVerifiedAt(now)

        Assert.assertEquals(Instant.parse("2025-06-15T05:00:00Z"), cutoff)
        Assert.assertEquals("2025-06-15T05:00:00Z", cutoff.toString())
    }
}
