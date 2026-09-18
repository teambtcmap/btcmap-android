package org.btcmap.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

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
    fun version100Database_isDiscardedAndRecreatedWithTheAreaTable() {
        val path = existingPath()
        // Reproduces the last release before areas were cached: version 100,
        // the then-current tables, and a row worth preserving if a migration
        // ever existed.
        createVersion100Database(path)

        val db = Database(BundledSQLiteDriver(), path)

        try {
            // The upgrade is a discard and recreate, not a migration: the old
            // data is gone and the new schema, including `area`, is built from
            // scratch. If someone adds a table without bumping VERSION, this
            // database is not discarded and the assertion on `area` fails.
            Assert.assertEquals(Database.VERSION, userVersion(db.conn))
            Assert.assertEquals(
                listOf("area", "comment", "event", "place", "pref"),
                tables(db.conn),
            )
            Assert.assertEquals(0L, db.place.selectCount())
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
     * The schema as of the last release before areas were cached: version 100
     * with place, event, comment and pref, but no `area`.
     */
    private fun createVersion100Database(path: String) {
        val conn = BundledSQLiteDriver().open(path)
        try {
            conn.execSQL(org.btcmap.db.table.place.CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(
                "INSERT INTO place (id, bundled, updated_at, lat, lon, icon) " +
                    "VALUES (1, 0, '2024-01-01T00:00:00Z', 0.0, 0.0, 'coffee');"
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
