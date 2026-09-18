package org.btcmap.db

import androidx.sqlite.SQLiteConnection
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
        createEventTableWithoutUpdatedAt(conn)
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
        createEventTableWithoutUpdatedAt(conn)
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

    @Test
    fun migration10_recreatesThePlaceIdIndexWithTheIdTieBreak() {
        val file = Files.createTempFile("btcmap-migration", ".db").toFile()
        file.deleteOnExit()
        val driver = BundledSQLiteDriver()

        val conn = driver.open(file.absolutePath)
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        // The version 9 index, before the id tie-break was added to the sort.
        conn.execSQL(
            "CREATE INDEX comment_place_id_created_at " +
                "ON comment(place_id, julianday(created_at) DESC);"
        )
        createEventTableWithoutUpdatedAt(conn)
        conn.execSQL("PRAGMA user_version=10;")
        conn.close()

        val db = Database(driver, file.absolutePath)

        val columns = mutableListOf<String?>()
        db.conn.prepare(
            "SELECT name FROM pragma_index_info('comment_place_id_created_at') ORDER BY seqno;"
        ).use {
            while (it.step()) {
                columns.add(it.getTextOrNull(0))
            }
        }

        // place_id, the julianday(created_at) expression (null name), id.
        Assert.assertEquals(listOf("place_id", null, "id"), columns)
    }

    @Test
    fun migration11_addsEventUpdatedAt() {
        val file = Files.createTempFile("btcmap-migration", ".db").toFile()
        file.deleteOnExit()
        val driver = BundledSQLiteDriver()

        val conn = driver.open(file.absolutePath)
        conn.execSQL(
            """
            CREATE TABLE event (
                id INTEGER PRIMARY KEY NOT NULL,
                area_id INTEGER,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                name TEXT NOT NULL,
                website TEXT,
                starts_at TEXT NOT NULL,
                ends_at TEXT
            );
            """
        )
        conn.execSQL(
            """
            INSERT INTO event (id, area_id, lat, lon, name, website, starts_at, ends_at)
            VALUES (1, NULL, 1.0, 2.0, 'Old', NULL, '2099-01-01T00:00:00Z', NULL);
            """
        )
        conn.execSQL("PRAGMA user_version=11;")
        conn.close()

        val db = Database(driver, file.absolutePath)

        // The migration backfills the sentinel so the first delta re-reads the
        // event and replaces it with the real updated_at.
        val old = db.event.selectById(1L)
        Assert.assertNotNull(old)
        Assert.assertEquals(ZonedDateTime.parse("2000-01-01T00:00:00Z"), old!!.updatedAt)

        db.event.insert(
            listOf(old.copy(updatedAt = ZonedDateTime.parse("2024-05-01T10:00:00Z")))
        )

        Assert.assertEquals(
            ZonedDateTime.parse("2024-05-01T10:00:00Z"),
            db.event.selectById(1L)!!.updatedAt,
        )
    }

    /**
     * A real database already has the event table by the time the later
     * migrations run. Tests that start mid-chain and only create the tables
     * they assert on must add it so migration 11 can alter it.
     */
    private fun createEventTableWithoutUpdatedAt(conn: SQLiteConnection) {
        conn.execSQL(
            """
            CREATE TABLE event (
                id INTEGER PRIMARY KEY NOT NULL,
                area_id INTEGER,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                name TEXT NOT NULL,
                website TEXT,
                starts_at TEXT NOT NULL,
                ends_at TEXT
            );
            """
        )
    }
}
