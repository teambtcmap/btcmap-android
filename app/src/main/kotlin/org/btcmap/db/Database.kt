package org.btcmap.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import kotlin.use
import org.btcmap.db.table.area.AreaQueries
import org.btcmap.db.table.comment.CommentQueries
import org.btcmap.db.table.event.EventQueries
import org.btcmap.db.table.place.PlaceQueries
import org.btcmap.db.table.preference.PreferenceQueries
import org.btcmap.db.table.user.UserStore
import java.io.File

class Database(driver: SQLiteDriver, val path: String) {
    companion object {
        /**
         * Schema version stamped onto fresh databases created by this app.
         *
         * Databases below [FIRST_OWN_VERSION] are discarded rather than
         * migrated; databases at or above it are migrated in place. The
         * boundary is deliberately far above the single-digit user_version of
         * the throwaway database that used the `btcmap.db` name early in the
         * project's history, so the stale file can be told apart from ours by
         * the pragma alone (see [initialize]).
         */
        const val VERSION = 103

        /**
         * The first version this app is responsible for upgrading. Anything
         * below it is the abandoned pre-1.0 `btcmap.db` and is deleted.
         */
        private const val FIRST_OWN_VERSION = 100

        private const val MEMORY_PATH = ":memory:"
        private const val USER_VERSION_QUERY = "SELECT user_version FROM pragma_user_version;"

        private val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")
    }

    val conn = initialize(driver, path)

    val place = PlaceQueries(conn)
    val comment = CommentQueries(conn)
    val event = EventQueries(conn)
    val area = AreaQueries(conn)
    val preference = PreferenceQueries(conn)
    val user = UserStore(preference)

    init {
        // [initialize] discards only foreign databases, so a version of 0 here
        // means the file was just created (or a creation was interrupted before
        // the version was stamped, in which case it was deleted and retried) and
        // the schema has to be built. A database at an older own version is
        // upgraded in place by [migrate].
        val version = readUserVersion(conn)
        if (version == 0) {
            createSchema(conn)
        } else if (version < VERSION) {
            transaction { migrate(conn, version) }
        }
    }

    /**
     * Opens [path], first removing a database left behind by an older app that
     * used a different schema.
     *
     * The `btcmap.db` name was once used for a database with unrelated tables,
     * so rather than migrating it we discard any file whose user_version is
     * below [FIRST_OWN_VERSION] (including a missing version, which reads as 0).
     * Its sidecar files are removed too. An unreadable file cannot be one of
     * ours either, so it is discarded as well. Databases at or above
     * [FIRST_OWN_VERSION] are kept and migrated.
     */
    private fun initialize(driver: SQLiteDriver, path: String): SQLiteConnection {
        if (path != MEMORY_PATH) {
            val file = File(path)
            if (file.exists() && readUserVersion(driver, path) < FIRST_OWN_VERSION) {
                file.delete()
                SIDECAR_SUFFIXES.forEach { suffix -> File("$path$suffix").delete() }
            }
        }
        return driver.open(path)
    }

    private fun readUserVersion(driver: SQLiteDriver, path: String): Int {
        return try {
            driver.open(path).use { readUserVersion(it) }
        } catch (_: Exception) {
            // Treat an unreadable file as stale rather than letting it block
            // every launch.
            -1
        }
    }

    private fun readUserVersion(conn: SQLiteConnection): Int {
        conn.prepare(USER_VERSION_QUERY).use {
            return if (it.step()) it.getInt(0) else 0
        }
    }

    private fun createSchema(conn: SQLiteConnection) {
        conn.execSQL(org.btcmap.db.table.place.CREATE)
        conn.execSQL(org.btcmap.db.table.event.CREATE)
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        conn.execSQL(org.btcmap.db.table.area.CREATE)
        conn.execSQL(org.btcmap.db.table.preference.CREATE)
        conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
        conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
        conn.execSQL("PRAGMA user_version=$VERSION;")
    }

    /**
     * Upgrades a database from its [from] version to [VERSION] in place.
     *
     * Each step advances the version by exactly one and must produce the schema
     * of that version, so historical statements are inlined where a shared
     * `CREATE` has since grown columns. A missing step is a programming error
     * and aborts rather than leaving a half-upgraded database behind; the
     * caller's next launch re-reads the version it stopped at.
     */
    private fun migrate(conn: SQLiteConnection, from: Int) {
        var version = from
        while (version < VERSION) {
            when (version) {
                100 -> {
                    // Areas are now cached locally. Inline the schema as of
                    // version 101 rather than area.CREATE: each migration must
                    // produce the schema of its own version, and area.CREATE
                    // has since grown the geo_json column added below.
                    conn.execSQL(
                        """
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
                    )
                }

                101 -> {
                    // Areas cache their full GeoJSON geometry, not just bbox.
                    // Reset updated_at to the sentinel so the next area sync
                    // re-reads every row and fills the new column: the cursor is
                    // just max(updated_at), and the existing rows would otherwise
                    // stay unenriched until they changed on the server.
                    conn.execSQL(
                        "ALTER TABLE ${org.btcmap.db.table.area.TABLE} " +
                            "ADD COLUMN ${org.btcmap.db.table.area.GEO_JSON} TEXT;"
                    )
                    conn.execSQL(
                        "UPDATE ${org.btcmap.db.table.area.TABLE} " +
                            "SET ${org.btcmap.db.table.area.UPDATED_AT} = '2000-01-01T00:00:00Z';"
                    )
                }

                102 -> {
                    // The bundled flag is gone: the snapshot now seeds complete
                    // records, so no place is ever marked bundled and the
                    // read-only "pending sync" state no longer exists. SQLite
                    // cannot drop a column on the oldest supported devices, so
                    // rebuild the table without it.
                    conn.execSQL(
                        """
                        CREATE TABLE place_new (
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
                    )
                    conn.execSQL(
                        """
                        INSERT INTO place_new (
                            id, updated_at, lat, lon, icon, name, localized_name, verified_at,
                            address, opening_hours, localized_opening_hours, phone, website,
                            email, twitter, facebook, instagram, line, required_app_url,
                            boosted_until, comments, telegram, osm_id, deleted_at
                        )
                        SELECT
                            id, updated_at, lat, lon, icon, name, localized_name, verified_at,
                            address, opening_hours, localized_opening_hours, phone, website,
                            email, twitter, facebook, instagram, line, required_app_url,
                            boosted_until, comments, telegram, osm_id, deleted_at
                        FROM place;
                        """
                    )
                    conn.execSQL("DROP TABLE place;")
                    conn.execSQL("ALTER TABLE place_new RENAME TO place;")
                }

                else -> throw Exception("migration is missing for version $version")
            }

            conn.execSQL("PRAGMA user_version=${++version};")
        }
    }

    /**
     * Runs [block] in a database transaction, rolling back if it throws.
     *
     * The raw `BEGIN`/`COMMIT` statements are mapped by the framework driver to
     * its real transaction machinery (see `SQLiteSession.executeSpecial`), so the
     * session pins a pooled connection for the duration of the block. This makes
     * concurrent transactions from different threads safe: each owns its own
     * connection and a write on another thread is a separate transaction that
     * cannot be rolled back by this one. Do not replace them with plain
     * statements or assume the connection is thread-confined.
     */
    fun transaction(block: () -> Unit) {
        conn.execSQL("BEGIN TRANSACTION;")
        try {
            block()
            conn.execSQL("COMMIT;")
        } catch (e: Throwable) {
            // Roll back on any failure, including an Error, and never let a
            // failed rollback mask the original exception.
            try {
                conn.execSQL("ROLLBACK;")
            } catch (_: Throwable) {
                // Ignored: the original exception is already on its way out.
            }
            throw e
        }
    }
}
