package org.btcmap.settings

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

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
}
