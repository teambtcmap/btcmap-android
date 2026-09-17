package org.btcmap.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.table.event.Event
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.time.ZonedDateTime

class DatabaseMigrationTest {
    @Test
    fun migration7_makesEventWebsiteNullable() {
        val file = Files.createTempFile("btcmap-migration", ".db").toFile()
        file.deleteOnExit()
        val driver = BundledSQLiteDriver()

        val conn = driver.open(file.absolutePath)
        // A real database at this version already has every table created at
        // version 0; the later migrations assume the ones they touch are there.
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        conn.execSQL(
            """
            CREATE TABLE event (
                id INTEGER PRIMARY KEY NOT NULL,
                area_id INTEGER,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                name TEXT NOT NULL,
                website TEXT NOT NULL,
                starts_at TEXT NOT NULL,
                ends_at TEXT
            );
            """
        )
        conn.execSQL(
            """
            INSERT INTO event (id, area_id, lat, lon, name, website, starts_at, ends_at)
            VALUES (1, NULL, 1.0, 2.0, 'Old', 'https://example.com', '2024-01-01T00:00:00Z', NULL);
            """
        )
        conn.execSQL("PRAGMA user_version=7;")
        conn.close()

        val db = Database(driver, file.absolutePath)

        val old = db.event.selectById(1L)
        Assert.assertNotNull(old)
        Assert.assertEquals("Old", old!!.name)
        Assert.assertEquals("https://example.com".toHttpUrl(), old.website)

        db.event.insert(
            listOf(
                Event(
                    id = 2L,
                    areaId = null,
                    lat = 3.0,
                    lon = 4.0,
                    name = "No Website",
                    website = null,
                    startsAt = ZonedDateTime.parse("2024-01-02T00:00:00Z"),
                    endsAt = null,
                )
            )
        )
        Assert.assertNull(db.event.selectById(2L)!!.website)
    }

    @Test
    fun migration8_createsPreferenceTable() {
        val file = Files.createTempFile("btcmap-migration", ".db").toFile()
        file.deleteOnExit()
        val driver = BundledSQLiteDriver()

        val conn = driver.open(file.absolutePath)
        // A real database at this version already has every table created at
        // version 0; migration 9 creates indexes on the comment table.
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        conn.execSQL("PRAGMA user_version=8;")
        conn.close()

        val db = Database(driver, file.absolutePath)

        db.preference.upsert("mapStyle", "dark")

        Assert.assertEquals("dark", db.preference.select("mapStyle"))
    }

    @Test
    fun migration9_createsCommentIndexes() {
        val file = Files.createTempFile("btcmap-migration", ".db").toFile()
        file.deleteOnExit()
        val driver = BundledSQLiteDriver()

        val conn = driver.open(file.absolutePath)
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        conn.execSQL("PRAGMA user_version=9;")
        conn.close()

        val db = Database(driver, file.absolutePath)

        val indexes = mutableSetOf<String>()
        db.conn.prepare("SELECT name FROM pragma_index_list('comment');").use {
            while (it.step()) {
                indexes.add(it.getText(0))
            }
        }

        Assert.assertTrue(indexes.contains("comment_place_id_created_at"))
        Assert.assertTrue(indexes.contains("comment_updated_at"))
    }
}
