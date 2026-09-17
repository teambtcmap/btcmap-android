package org.btcmap.db.table.preference

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test

class PreferenceQueriesTest {

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    @Test
    fun upsert_and_select() {
        val db = createDatabase()

        db.preference.upsert("mapStyle", "dark")

        Assert.assertEquals("dark", db.preference.select("mapStyle"))
    }

    @Test
    fun select_returnsNullWhenAbsent() {
        val db = createDatabase()

        Assert.assertNull(db.preference.select("mapStyle"))
    }

    @Test
    fun upsert_replacesExistingValue() {
        val db = createDatabase()

        db.preference.upsert("mapStyle", "dark")
        db.preference.upsert("mapStyle", "bright")

        Assert.assertEquals("bright", db.preference.select("mapStyle"))
    }

    @Test
    fun selectAll_returnsEveryRow() {
        val db = createDatabase()

        db.preference.upsert("mapStyle", "dark")
        db.preference.upsert("verified_filter_years", "1")

        Assert.assertEquals(
            mapOf("mapStyle" to "dark", "verified_filter_years" to "1"),
            db.preference.selectAll(),
        )
    }

    @Test
    fun delete_removesKey() {
        val db = createDatabase()
        db.preference.upsert("mapStyle", "dark")

        db.preference.delete("mapStyle")

        Assert.assertNull(db.preference.select("mapStyle"))
    }

    @Test
    fun delete_doesNothingWhenAbsent() {
        val db = createDatabase()

        db.preference.delete("mapStyle")

        Assert.assertNull(db.preference.select("mapStyle"))
    }

    @Test
    fun deleteAll_removesEveryRow() {
        val db = createDatabase()
        db.preference.upsert("mapStyle", "dark")
        db.preference.upsert("verified_filter_years", "1")

        db.preference.deleteAll()

        Assert.assertTrue(db.preference.selectAll().isEmpty())
    }
}
