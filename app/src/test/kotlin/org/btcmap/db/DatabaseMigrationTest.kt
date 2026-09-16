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
}
