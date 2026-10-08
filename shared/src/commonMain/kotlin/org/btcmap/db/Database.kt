package org.btcmap.db

import kotlinx.coroutines.sync.withLock
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import kotlin.use
import okio.Path.Companion.toPath
import org.btcmap.db.table.area.AreaQueries
import org.btcmap.db.table.comment.CommentQueries
import org.btcmap.db.table.event.EventQueries
import org.btcmap.db.table.note.NoteQueries
import org.btcmap.db.table.place.PlaceQueries
import org.btcmap.db.table.preference.PreferenceQueries
import org.btcmap.db.table.user.UserStore
import org.btcmap.io.platformFileSystem
import org.btcmap.platform.PlatformLock

class Database(driver: SQLiteDriver, val path: String) {
    companion object {
        /**
         * Schema version stamped onto fresh databases created by this app.
         *
         * Databases below [FIRST_OWN_VERSION] are discarded rather than
         * migrated, and so are databases from a newer app version (above this
         * value); a database in between is migrated in place. The lower
         * boundary is deliberately far above the single-digit user_version of
         * the throwaway database that used the `btcmap.db` name early in the
         * project's history, so the stale file can be told apart from ours by
         * the pragma alone (see [initialize]).
         */
        const val VERSION = 109

        /**
         * The first version this app is responsible for upgrading. Anything
         * below it is the abandoned pre-1.0 `btcmap.db` and is deleted.
         */
        private const val FIRST_OWN_VERSION = 100

        private const val MEMORY_PATH = ":memory:"
        private const val USER_VERSION_QUERY = "SELECT user_version FROM pragma_user_version;"

        private val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")

        /**
         * Whether opening [path] would run an in-place [migrate], which can
         * rewrite whole tables. Callers that open the database on the main
         * thread (to load the settings before the first screen) use this to
         * keep that work off it. A missing, unreadable, below-[FIRST_OWN_VERSION]
         * or above-[VERSION] file is not a migration: it is created or
         * discarded, which is cheap.
         */
        suspend fun needsMigration(driver: SQLiteDriver, path: String): Boolean {
            val version = readUserVersion(driver, path)
            return version >= FIRST_OWN_VERSION && version < VERSION
        }

        private suspend fun readUserVersion(driver: SQLiteDriver, path: String): Int {
            return try {
                driver.open(path).use { readUserVersion(it) }
            } catch (_: Exception) {
                // Treat an unreadable file as stale rather than letting it block
                // every launch.
                -1
            }
        }

        private suspend fun readUserVersion(conn: SQLiteConnection): Int {
            conn.prepare(USER_VERSION_QUERY).use {
                return if (it.step()) it.getInt(0) else 0
            }
        }
    }

    private val driver = driver

    /**
     * The single connection every query object in this database shares. Wrapped
     * in the platform's serialization layer by [openDatabaseConnection]; the
     * public type stays [SQLiteConnection] so the wrapper is an implementation
     * detail. Only available after [connect].
     */
    val conn: SQLiteConnection
        get() = connection ?: error("Database.connect() has not been called")

    private var connection: SQLiteConnection? = null

    lateinit var place: PlaceQueries
        private set
    lateinit var comment: CommentQueries
        private set
    lateinit var event: EventQueries
        private set
    lateinit var area: AreaQueries
        private set
    lateinit var note: NoteQueries
        private set
    lateinit var preference: PreferenceQueries
        private set
    lateinit var user: UserStore
        private set

    /**
     * Guards [transaction] against reentrancy on the calling thread. It is a
     * dedicated lock rather than the connection's, so an open statement on the
     * same thread (which holds the connection lock) does not look like a nested
     * transaction.
     */
    private val transactionLock = PlatformLock()

    /** Serializes opening the connection so two concurrent callers cannot race. */
    private val connectMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Opens the connection and prepares the schema. It is a separate step
     * because opening and migrating are suspending on the web target.
     *
     * [initialize] discards every database this version cannot upgrade (foreign,
     * unreadable or from a newer build), so a version of 0 here means the file
     * was just created (or a creation was interrupted before the version was
     * stamped, in which case it was deleted and retried) and the schema has to
     * be built. A database at an older own version is upgraded in place.
     */
    suspend fun connect() {
        if (connection != null) return

        connectMutex.withLock {
            if (connection != null) return

            val conn = initialize(driver, path)
            connection = conn
            place = PlaceQueries(conn)
            comment = CommentQueries(conn)
            event = EventQueries(conn)
            area = AreaQueries(conn)
            note = NoteQueries(conn)
            preference = PreferenceQueries(conn)
            user = UserStore(preference)

            val version = readUserVersion(conn)
            if (version == 0) {
                createSchema(conn)
            } else if (version < VERSION) {
                transaction { migrate(conn, version) }
            }
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
     * [FIRST_OWN_VERSION] and at or below [VERSION] are kept and migrated; a
     * database left by a newer app version cannot be read safely and is
     * discarded like a foreign one.
     */
    private suspend fun initialize(driver: SQLiteDriver, path: String): SQLiteConnection {
        if (path != MEMORY_PATH) {
            val fileSystem = platformFileSystem
            if (fileSystem != null) {
                val file = path.toPath()
                if (fileSystem.exists(file) && isDiscardable(readUserVersion(driver, path))) {
                    fileSystem.delete(file)
                    SIDECAR_SUFFIXES.forEach { suffix ->
                        val sidecar = "$path$suffix".toPath()
                        if (fileSystem.exists(sidecar)) fileSystem.delete(sidecar)
                    }
                }
            }
        }
        return openDatabaseConnection(driver, path)
    }

    /**
     * Whether a database at [version] belongs to another app or a newer build
     * and must be recreated instead of migrated. An unreadable or version-less
     * file reads as a version below [FIRST_OWN_VERSION].
     */
    private fun isDiscardable(version: Int): Boolean =
        version < FIRST_OWN_VERSION || version > VERSION

    private suspend fun createSchema(conn: SQLiteConnection) {
        conn.execSQL(org.btcmap.db.table.place.CREATE)
        conn.execSQL(org.btcmap.db.table.event.CREATE)
        conn.execSQL(org.btcmap.db.table.comment.CREATE)
        conn.execSQL(org.btcmap.db.table.area.CREATE)
        conn.execSQL(org.btcmap.db.table.note.CREATE)
        conn.execSQL(org.btcmap.db.table.preference.CREATE)
        conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
        conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
        conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
        conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
        conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
        conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
        conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
        conn.execSQL(org.btcmap.db.table.area.CREATE_INDEX_UPDATED_AT)
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
    private suspend fun migrate(conn: SQLiteConnection, from: Int) {
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

                103 -> {
                    // The place, event and area sync cursors are read with
                    // ORDER BY julianday(updated_at) at the start of every sync,
                    // and the map and issue screens filter by bounds and osm_id;
                    // add the indexes that back those reads, matching the
                    // comment table's. The expression index lets SQLite read the
                    // newest row directly instead of scanning and sorting.
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
                    conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
                    conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
                    conn.execSQL(org.btcmap.db.table.area.CREATE_INDEX_UPDATED_AT)
                }

                104 -> {
                    // The event's legacy area_id column is gone: the v4 payload
                    // never carried one and the event-to-area link is resolved
                    // geometrically, so the column was always null. SQLite
                    // cannot drop a column on the oldest supported devices, so
                    // rebuild the table without it; the drop also removes the
                    // indexes, which are recreated below.
                    conn.execSQL(
                        """
                        CREATE TABLE event_new (
                            id INTEGER PRIMARY KEY NOT NULL,
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
                    )
                    conn.execSQL(
                        """
                        INSERT INTO event_new (
                            id, lat, lon, name, website,
                            starts_at, ends_at, updated_at, deleted_at
                        )
                        SELECT
                            id, lat, lon, name, website,
                            starts_at, ends_at, updated_at, deleted_at
                        FROM event;
                        """
                    )
                    conn.execSQL("DROP TABLE event;")
                    conn.execSQL("ALTER TABLE event_new RENAME TO event;")
                    conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_UPDATED_AT)
                    conn.execSQL(org.btcmap.db.table.event.CREATE_INDEX_BOUNDS)
                }

                105 -> {
                    // Areas now cache their per-language names and descriptions
                    // so the UI can localize offline like it already does for
                    // places. Add the columns and clear the cached areas so the
                    // refreshed bundled snapshot re-seeds them with the new
                    // fields on the next sync.
                    //
                    // Clearing beats merely rewinding the sync cursor: rewinding
                    // would make the delta sync re-download every area's polygon
                    // from the API, while the seed reads the asset offline and
                    // leaves only a small delta. It is safe because areas are a
                    // pure cache, saved areas live server-side, and the delta
                    // sync re-adds any tombstone newer than the snapshot.
                    conn.execSQL(
                        "ALTER TABLE ${org.btcmap.db.table.area.TABLE} " +
                            "ADD COLUMN ${org.btcmap.db.table.area.LOCALIZED_NAME} TEXT;"
                    )
                    conn.execSQL(
                        "ALTER TABLE ${org.btcmap.db.table.area.TABLE} " +
                            "ADD COLUMN ${org.btcmap.db.table.area.LOCALIZED_DESCRIPTION} TEXT;"
                    )
                    conn.execSQL("DELETE FROM ${org.btcmap.db.table.area.TABLE};")
                }

                106 -> {
                    // The per-language opening-hours map is gone: the API's
                    // human-readable variants were never translated fully, so
                    // the raw `opening_hours` is shown instead. SQLite cannot
                    // drop a column on the oldest supported devices, so rebuild
                    // the table without it.
                    //
                    // The rows are discarded rather than copied: places are a
                    // pure cache and an empty table is re-seeded from the
                    // bundled snapshot on the next sync, so the column can go
                    // without re-downloading every place from the API. The drop
                    // also removes the indexes, which are recreated below.
                    conn.execSQL("DROP TABLE ${org.btcmap.db.table.place.TABLE};")
                    conn.execSQL(
                        """
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
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_UPDATED_AT)
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_OSM_ID)
                    conn.execSQL(org.btcmap.db.table.place.CREATE_INDEX_BOUNDS)
                }

                107 -> {
                    // Areas now cache their last verification date. Add the
                    // column and clear the cached areas so the refreshed bundled
                    // snapshot re-seeds them with it: the delta sync would only
                    // fill it for areas that change afterwards, leaving every
                    // already-cached area looking unverified. Clearing is safe
                    // for the same reasons as the 105 migration (areas are a
                    // pure cache; the seed reads the asset offline).
                    conn.execSQL(
                        "ALTER TABLE ${org.btcmap.db.table.area.TABLE} " +
                            "ADD COLUMN ${org.btcmap.db.table.area.VERIFIED_AT} TEXT;"
                    )
                    conn.execSQL("DELETE FROM ${org.btcmap.db.table.area.TABLE};")
                }

                108 -> {
                    // The signed-in user's personal notes are now cached so the
                    // sync rewrites them in place and the My-notes screen reads
                    // from the database instead of fetching on every open.
                    conn.execSQL(org.btcmap.db.table.note.CREATE)
                }

                else -> throw Exception("migration is missing for version $version")
            }

            conn.execSQL("PRAGMA user_version=${++version};")
        }
    }

    /**
     * Runs [block] in a database transaction, rolling back if it throws.
     *
     * The connection's serialization lock is held for the whole block (see
     * [LockingSQLiteConnection.transaction]), so no other statement — on this or
     * any other thread — can interleave with the transaction. Concurrent
     * transactions from different threads are therefore serialized instead of
     * racing for the connection.
     *
     * Nesting is not supported: a second [transaction] on the same thread throws
     * instead of silently reusing the outer transaction.
     */
    suspend fun transaction(block: suspend () -> Unit) {
        check(!transactionLock.isHeldByCurrentThread()) { "Database.transaction cannot be nested" }
        transactionLock.lock()
        try {
            withTransaction(conn, block)
        } finally {
            transactionLock.unlock()
        }
    }

    /** Closes the underlying connection. The app holds one for the process. */
    fun close() {
        conn.close()
    }
}
