package org.btcmap.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.ZonedDateTime

/** The `place` table as it shipped in schema version 102, with `bundled`. */
private const val VERSION_102_PLACE_CREATE = """
    CREATE TABLE place (
        id INTEGER PRIMARY KEY NOT NULL,
        bundled INTEGER NOT NULL,
        updated_at TEXT NOT NULL,
        lat REAL NOT NULL,
        lon REAL NOT NULL,
        icon TEXT NOT NULL,
        name TEXT,
        localized_name TEXT,
        verified_at TEXT,
        address TEXT,
        opening_hours TEXT,
        localized_opening_hours TEXT,
        phone TEXT,
        website TEXT,
        email TEXT,
        twitter TEXT,
        facebook TEXT,
        instagram TEXT,
        line TEXT,
        required_app_url TEXT,
        boosted_until TEXT,
        comments INTEGER,
        telegram TEXT,
        osm_id TEXT,
        deleted_at TEXT
    );
"""

/** The `area` table as it shipped in schema version 101, without `geo_json`. */
private const val VERSION_101_AREA_CREATE = """
    CREATE TABLE area (
        id INTEGER PRIMARY KEY NOT NULL,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        url_alias TEXT NOT NULL,
        icon TEXT,
        icon_wide TEXT,
        website_url TEXT NOT NULL,
        description TEXT,
        bbox_west REAL,
        bbox_south REAL,
        bbox_east REAL,
        bbox_north REAL,
        updated_at TEXT NOT NULL,
        deleted_at TEXT
    );
"""

class DatabaseTest {

    @Test
    fun newDatabase_createsTheSchemaAtTheCurrentVersion() {
        val db = Database(BundledSQLiteDriver(), newPath())

        try {
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertEquals(
                listOf("area", "comment", "event", "place", "pref"),
                tables(db.conn),
            )
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun staleDatabase_isDiscardedAndRecreated() {
        val path = existingPath()
        createStaleDatabase(path, version = 1)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            // The stale table is gone because the file was deleted rather than
            // migrated, and the fresh schema was created in its place.
            Assert.assertFalse(hasTable(db.conn, "stale"))
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version100Database_isMigratedWithTheAreaTable() {
        val path = existingPath()
        // Reproduces the last schema before areas were cached: version 100 with
        // the then-current tables, and a row worth preserving.
        createVersion100Database(path)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            // Our own databases are upgraded in place, not discarded: the
            // existing place survives and the area table is added by migration.
            // If someone bumps VERSION without adding a step, this database is
            // not discarded and the assertion on `area` fails.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertEquals(
                listOf("area", "comment", "event", "place", "pref"),
                tables(db.conn),
            )
            Assert.assertEquals(1L, db.place.selectCount())
            Assert.assertEquals(0L, db.area.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version101Database_isMigratedWithGeoJson() {
        val path = existingPath()
        // Reproduces the last schema before geo_json was cached: version 101
        // with the area table, but no geo_json column on it.
        createVersion101Database(path)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            // The migration adds the column in place, keeping existing rows, and
            // rewinds each row's updated_at to the sentinel so the next sync
            // re-reads it and fills geo_json.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertTrue(areaColumns(db.conn).contains("geo_json"))
            Assert.assertEquals(1L, db.area.selectCount())
            Assert.assertEquals(
                ZonedDateTime.parse("2000-01-01T00:00:00Z"),
                db.area.selectMaxUpdatedAt(),
            )
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version102Database_isMigratedWithoutTheBundledColumn() {
        val path = existingPath()
        // Reproduces the last schema before the bundled flag was dropped:
        // version 102 with a place table that still carries the column.
        createVersion102Database(path)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            // The table is rebuilt without the column in place, keeping the
            // existing row.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertFalse(placeColumns(db.conn).contains("bundled"))
            val place = db.place.selectById(1L)
            Assert.assertNotNull(place)
            Assert.assertEquals("Cafe", place!!.name)
            Assert.assertEquals(1L, db.place.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun databaseWithoutAVersion_isDiscardedAndRecreated() {
        val path = existingPath()
        createStaleDatabase(path, version = null)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            Assert.assertFalse(hasTable(db.conn, "stale"))
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun unreadableDatabase_isDiscardedAndRecreated() {
        val path = existingPath()
        File(path).writeBytes(byteArrayOf(1, 2, 3, 4))

        val db = Database(BundledSQLiteDriver(), path)

        try {
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun currentDatabase_isReopenedWithoutLosingData() {
        val path = existingPath()

        val first = Database(BundledSQLiteDriver(), path)
        first.preference.upsert("mapStyle", "dark")
        first.conn.close()

        val second = Database(BundledSQLiteDriver(), path)

        try {
            Assert.assertEquals("dark", second.preference.select("mapStyle"))
            Assert.assertEquals(Database.VERSION, userVersion(second.conn))
        } finally {
            second.conn.close()
        }
    }

    private fun newPath(): String {
        val file = File(existingPath())
        Assert.assertTrue(file.delete())
        return file.absolutePath
    }

    private fun existingPath(): String {
        val file = Files.createTempFile("btcmap-test", ".db").toFile()
        file.deleteOnExit()
        return file.absolutePath
    }

    private fun createStaleDatabase(path: String, version: Int?) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL("CREATE TABLE stale (id INTEGER PRIMARY KEY, tags TEXT NOT NULL);")
            conn.execSQL("INSERT INTO stale (id, tags) VALUES (1, '{}');")
            if (version != null) {
                conn.execSQL("PRAGMA user_version=$version;")
            }
        } finally {
            conn.close()
        }
    }

    /**
     * The schema as of the last own version before areas were cached: version
     * 100 with place, event, comment and pref, but no `area`.
     */
    private fun createVersion100Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(org.btcmap.db.table.place.CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(
                "INSERT INTO place (id, updated_at, lat, lon, icon) " +
                    "VALUES (1, '2024-01-01T00:00:00Z', 0.0, 0.0, 'coffee');"
            )
            conn.execSQL("PRAGMA user_version=100;")
        } finally {
            conn.close()
        }
    }

    private fun userVersion(conn: SQLiteConnection): Int {
        conn.prepare("SELECT user_version FROM pragma_user_version;").use {
            it.step()
            return it.getInt(0)
        }
    }

    /**
     * The schema as of the last release before geo_json was cached: version 101
     * with place, event, comment, area and pref. The area table predates the
     * geo_json column.
     */
    private fun createVersion101Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(org.btcmap.db.table.place.CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(VERSION_101_AREA_CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(
                "INSERT INTO area (id, name, type, url_alias, website_url, updated_at) " +
                    "VALUES (1, 'Grand Paris', 'community', 'grand-paris', " +
                    "'https://btcmap.org/community/grand-paris', '2024-01-01T00:00:00Z');"
            )
            conn.execSQL("PRAGMA user_version=101;")
        } finally {
            conn.close()
        }
    }

    /**
     * The schema as of the last release before the bundled flag was dropped:
     * version 102, with a place table that still carries the `bundled` column.
     */
    private fun createVersion102Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(VERSION_102_PLACE_CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(org.btcmap.db.table.area.CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(
                "INSERT INTO place (id, bundled, updated_at, lat, lon, icon, name) " +
                    "VALUES (1, 0, '2024-01-01T00:00:00Z', 0.0, 0.0, 'local_cafe', 'Cafe');"
            )
            conn.execSQL("PRAGMA user_version=102;")
        } finally {
            conn.close()
        }
    }

    private fun areaColumns(conn: SQLiteConnection): List<String> =
        columns(conn, "area")

    private fun placeColumns(conn: SQLiteConnection): List<String> =
        columns(conn, "place")

    private fun columns(conn: SQLiteConnection, table: String): List<String> {
        val names = mutableListOf<String>()
        conn.prepare("SELECT name FROM pragma_table_info('$table');").use {
            while (it.step()) {
                names.add(it.getText(0))
            }
        }
        return names
    }

    private fun hasTable(conn: SQLiteConnection, name: String): Boolean =
        tables(conn).contains(name)

    private fun tables(conn: SQLiteConnection): List<String> {
        val names = mutableListOf<String>()
        conn.prepare(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table' AND name NOT LIKE 'sqlite_%'
            ORDER BY name;
            """
        ).use {
            while (it.step()) {
                names.add(it.getText(0))
            }
        }
        return names
    }
}
