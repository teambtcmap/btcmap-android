package org.btcmap.db

import kotlinx.coroutines.runBlocking
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

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

/**
 * The `area` table as it stood through schema version 105, before the
 * per-language `localized_name`/`localized_description` columns were added.
 * Version 106 appends those columns in place.
 */
private const val VERSION_105_AREA_CREATE = """
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
        geo_json TEXT,
        updated_at TEXT NOT NULL,
        deleted_at TEXT
    );
"""

/**
 * The `event` table as it shipped through schema version 104, with the legacy
 * `area_id` column. Migration 104 rebuilds it without that column.
 */
private const val VERSION_104_EVENT_CREATE = """
    CREATE TABLE event (
        id INTEGER PRIMARY KEY NOT NULL,
        area_id INTEGER,
        lat REAL NOT NULL,
        lon REAL NOT NULL,
        name TEXT NOT NULL,
        website TEXT,
        starts_at TEXT NOT NULL,
        ends_at TEXT,
        updated_at TEXT NOT NULL,
        deleted_at TEXT
    );
"""

/**
 * The `place` table as it shipped through schema version 106, before
 * `localized_opening_hours` was dropped. Migration 106 rebuilds it without the
 * column. The earlier version helpers use it too: those releases shipped the
 * same columns (only 102 adds the legacy `bundled` flag, which has its own
 * constant), so a rebuilt table proves the column was actually removed.
 */
private const val VERSION_106_PLACE_CREATE = """
    CREATE TABLE place (
        id INTEGER PRIMARY KEY NOT NULL,
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

class DatabaseTest {

    @Test
    fun newDatabase_createsTheSchemaAtTheCurrentVersion() = runBlocking<Unit> {
        val db = Database(BundledSQLiteDriver(), newPath()).apply { connect() }

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
    fun staleDatabase_isDiscardedAndRecreated() = runBlocking<Unit> {
        val path = existingPath()
        createStaleDatabase(path, version = 1)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

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
    fun version100Database_isMigratedWithTheAreaTable() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before areas were cached: version 100 with
        // the then-current tables, and a row worth preserving.
        createVersion100Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // Our own databases are upgraded in place, not discarded: the
            // schema reaches the current version and the area table is added by
            // migration. If someone bumps VERSION without adding a step, this
            // database is not discarded and the assertion on `area` fails.
            // The place cache ends up empty because the final migration empties
            // it so the bundled snapshot re-seeds it.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertEquals(
                listOf("area", "comment", "event", "place", "pref"),
                tables(db.conn),
            )
            Assert.assertEquals(0L, db.place.selectCount())
            Assert.assertEquals(0L, db.area.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version101Database_isMigratedWithGeoJson() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before geo_json was cached: version 101
        // with the area table, but no geo_json column on it.
        createVersion101Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // Migration 101 adds the geo_json column. This opens at the current
            // VERSION, so the later area migrations run too: 105 clears the
            // cache to force a re-seed, leaving the table empty at the end.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertTrue(areaColumns(db.conn).contains("geo_json"))
            Assert.assertEquals(0L, db.area.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version102Database_isMigratedWithoutTheBundledColumn() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before the bundled flag was dropped:
        // version 102 with a place table that still carries the column.
        createVersion102Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // The table is rebuilt without the `bundled` column. The place
            // cache ends up empty because the final migration empties it so the
            // bundled snapshot re-seeds it.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertFalse(placeColumns(db.conn).contains("bundled"))
            Assert.assertEquals(0L, db.place.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version103Database_isMigratedWithTheNewIndexes() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before the place, event and area indexes
        // were added: version 103 with the current tables but only the comment
        // indexes.
        createVersion103Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_updated_at"))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_osm_id"))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_bounds"))
            Assert.assertTrue(indexes(db.conn, "event").contains("event_updated_at"))
            Assert.assertTrue(indexes(db.conn, "event").contains("event_bounds"))
            Assert.assertTrue(indexes(db.conn, "area").contains("area_updated_at"))
            // The place cache ends up empty because the final migration empties
            // it so the bundled snapshot re-seeds it; the recreated indexes are
            // still in place.
            Assert.assertEquals(0L, db.place.selectCount())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version104Database_isMigratedWithoutTheLegacyEventAreaIdColumn() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before the event area_id column was
        // dropped: version 104 with event still carrying the column.
        createVersion104Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // The table is rebuilt without the column in place, keeping the
            // existing row and recreating the indexes the rebuild dropped.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertFalse(eventColumns(db.conn).contains("area_id"))
            val event = db.event.selectById(1L)
            Assert.assertNotNull(event)
            Assert.assertEquals("Meetup", event!!.name)
            Assert.assertTrue(indexes(db.conn, "event").contains("event_updated_at"))
            Assert.assertTrue(indexes(db.conn, "event").contains("event_bounds"))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version105Database_isMigratedByClearingTheAreaCache() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before areas cached per-language names and
        // descriptions: version 105 with the area table but without the
        // localized columns.
        createVersion105Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // The migration adds the columns and empties the cache, so the next
            // sync re-seeds it from the refreshed bundled snapshot (which now
            // carries the per-language fields) instead of re-downloading every
            // area's polygon. An empty table also resets the delta cursor.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertTrue(areaColumns(db.conn).contains("localized_name"))
            Assert.assertTrue(areaColumns(db.conn).contains("localized_description"))
            Assert.assertEquals(0L, db.area.selectCount(includeDeleted = true))
            Assert.assertNull(db.area.selectMaxUpdatedAt())
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun version106Database_isMigratedWithoutTheLocalizedOpeningHoursColumn() = runBlocking<Unit> {
        val path = existingPath()
        // Reproduces the last schema before the per-language opening-hours map
        // was dropped: version 106 with place still carrying the column.
        createVersion106Database(path)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // The table is rebuilt without the column and emptied, so the
            // bundled snapshot re-seeds it on the next sync; the rebuild drops
            // the indexes, which are recreated.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertFalse(placeColumns(db.conn).contains("localized_opening_hours"))
            Assert.assertEquals(0L, db.place.selectCount(includeDeleted = true))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_updated_at"))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_osm_id"))
            Assert.assertTrue(indexes(db.conn, "place").contains("place_bounds"))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun newerDatabase_isDiscardedAndRecreated() = runBlocking<Unit> {
        val path = existingPath()
        createStaleDatabase(path, version = Database.VERSION + 1)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            // A database from a newer build cannot be read safely, so it is
            // recreated like a foreign one instead of being opened as-is.
            Assert.assertFalse(hasTable(db.conn, "stale"))
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun databaseWithoutAVersion_isDiscardedAndRecreated() = runBlocking<Unit> {
        val path = existingPath()
        createStaleDatabase(path, version = null)

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            Assert.assertFalse(hasTable(db.conn, "stale"))
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun needsMigration_isTrueForAnOlderOwnVersion() = runBlocking<Unit> {
        val path = existingPath()
        createVersion100Database(path)

        Assert.assertTrue(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun needsMigration_isTrueForTheNewestMigratableVersion() = runBlocking<Unit> {
        val path = existingPath()
        createVersion106Database(path)

        Assert.assertTrue(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun needsMigration_isFalseForACurrentDatabase() = runBlocking<Unit> {
        val path = existingPath()
        Database(BundledSQLiteDriver(), path).apply { connect() }.conn.close()

        Assert.assertFalse(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun needsMigration_isFalseForAMissingDatabase() = runBlocking<Unit> {
        // A missing file is created rather than migrated, so it is cheap enough
        // to open on the main thread.
        val path = newPath()

        Assert.assertFalse(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun needsMigration_isFalseForADatabaseThatIsDiscarded() = runBlocking<Unit> {
        // A file from an unrelated app is deleted and recreated, not migrated.
        val path = existingPath()
        createStaleDatabase(path, version = 1)

        Assert.assertFalse(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun needsMigration_isFalseForANewerDatabase() = runBlocking<Unit> {
        // A newer database is recreated, not migrated, so it is cheap enough to
        // open on the main thread.
        val path = existingPath()
        createStaleDatabase(path, version = Database.VERSION + 1)

        Assert.assertFalse(Database.needsMigration(BundledSQLiteDriver(), path))
    }

    @Test
    fun unreadableDatabase_isDiscardedAndRecreated() = runBlocking<Unit> {
        val path = existingPath()
        File(path).writeBytes(byteArrayOf(1, 2, 3, 4))

        val db = Database(BundledSQLiteDriver(), path).apply { connect() }

        try {
            Assert.assertTrue(hasTable(db.conn, "place"))
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
        } finally {
            db.conn.close()
        }
    }

    @Test
    fun currentDatabase_isReopenedWithoutLosingData() = runBlocking<Unit> {
        val path = existingPath()

        val first = Database(BundledSQLiteDriver(), path).apply { connect() }
        first.preference.upsert("mapStyle", "dark")
        first.conn.close()

        val second = Database(BundledSQLiteDriver(), path).apply { connect() }

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
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(VERSION_104_EVENT_CREATE)
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
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(VERSION_104_EVENT_CREATE)
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
            conn.execSQL(VERSION_104_EVENT_CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(VERSION_105_AREA_CREATE)
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

    /**
     * The schema as of version 103: the current tables and comment indexes,
     * before the place, event and area indexes were added.
     */
    private fun createVersion103Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(VERSION_104_EVENT_CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(VERSION_105_AREA_CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(
                "INSERT INTO place (id, updated_at, lat, lon, icon) " +
                    "VALUES (1, '2024-01-01T00:00:00Z', 0.0, 0.0, 'coffee');"
            )
            conn.execSQL("PRAGMA user_version=103;")
        } finally {
            conn.close()
        }
    }

    /**
     * The schema as of version 104: the current tables with the event table
     * still carrying its legacy `area_id` column.
     */
    private fun createVersion104Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(VERSION_104_EVENT_CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(VERSION_105_AREA_CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.area.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(
                "INSERT INTO event (id, area_id, lat, lon, name, starts_at, updated_at) " +
                    "VALUES (1, 42, 1.0, 2.0, 'Meetup', '2099-01-01T00:00:00Z', " +
                    "'2024-01-01T00:00:00Z');"
            )
            conn.execSQL("PRAGMA user_version=104;")
        } finally {
            conn.close()
        }
    }

    /**
     * The schema as of version 105: the current tables, with the area table
     * still missing the per-language name/description columns.
     */
    private fun createVersion105Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(VERSION_105_AREA_CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.area.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(
                "INSERT INTO area (id, name, type, url_alias, website_url, updated_at) " +
                    "VALUES (1, 'Grand Paris', 'community', 'grand-paris', " +
                    "'https://btcmap.org/community/grand-paris', '2024-01-01T00:00:00Z');"
            )
            conn.execSQL("PRAGMA user_version=105;")
        } finally {
            conn.close()
        }
    }

    /**
     * The schema as of version 106: the current tables, with the place table
     * still carrying the per-language opening-hours column.
     */
    private fun createVersion106Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(VERSION_106_PLACE_CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(org.btcmap.db.table.area.CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
            conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
            conn.execSQL(org.btcmap.db.table.area.CREATE_INDEX_UPDATED_AT)
            conn.execSQL(
                "INSERT INTO place (id, updated_at, lat, lon, icon, name, opening_hours, " +
                    "localized_opening_hours) VALUES (1, '2024-01-01T00:00:00Z', 0.0, 0.0, " +
                    "'local_cafe', 'Cafe', 'Mo-Fr 08:00-18:00', '{\"en\":\"Mo-Fr 08:00-18:00\"}');"
            )
            conn.execSQL("PRAGMA user_version=106;")
        } finally {
            conn.close()
        }
    }

    private fun areaColumns(conn: SQLiteConnection): List<String> =
        columns(conn, "area")

    private fun eventColumns(conn: SQLiteConnection): List<String> =
        columns(conn, "event")

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

    private fun indexes(conn: SQLiteConnection, table: String): List<String> {
        val names = mutableListOf<String>()
        conn.prepare("SELECT name FROM pragma_index_list('$table');").use {
            while (it.step()) {
                names.add(it.getText(0))
            }
        }
        return names
    }

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
