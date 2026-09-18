package org.btcmap.db

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import org.btcmap.db.table.comment.CommentQueries
import org.btcmap.db.table.event.EventQueries
import org.btcmap.db.table.place.PlaceQueries
import org.btcmap.db.table.preference.PreferenceQueries
import org.btcmap.db.table.user.UserQueries

class Database(driver: SQLiteDriver, val path: String) {
    companion object {
        private const val VERSION = 12
    }

    val conn = driver.open(path)

    val place = PlaceQueries(conn)
    val comment = CommentQueries(conn)
    val event = EventQueries(conn)
    val user = UserQueries(conn)
    val preference = PreferenceQueries(conn)

    init {
        migrate()
    }

    private fun migrate() {
        val stmt = conn.prepare("SELECT user_version FROM pragma_user_version;")
        var version = if (stmt.step()) stmt.getInt(0) else 0
        stmt.reset()

        if (version == 0) {
            conn.execSQL(org.btcmap.db.table.place.CREATE)
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE)
            conn.execSQL(org.btcmap.db.table.user.CREATE)
            conn.execSQL(org.btcmap.db.table.preference.CREATE)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
            conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
            conn.execSQL("PRAGMA user_version=$VERSION;")
            return
        }

        while (version < VERSION) {
            when (version) {
                1 -> {
                    conn.execSQL("ALTER TABLE place ADD COLUMN localized_name TEXT;")
                    conn.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
                }

                2 -> {
                    conn.execSQL("ALTER TABLE place ADD COLUMN localized_opening_hours TEXT;")
                    conn.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
                }

                3 -> {
                    conn.execSQL(org.btcmap.db.table.user.CREATE)
                }

                4 -> {
                    conn.execSQL("ALTER TABLE event ADD COLUMN area_id INTEGER;")
                    conn.execSQL("ALTER TABLE event ADD COLUMN cron_schedule TEXT;")
                }

                5 -> {
                    conn.execSQL("ALTER TABLE event DROP COLUMN cron_schedule;")
                }

                6 -> {
                    conn.execSQL("ALTER TABLE place ADD COLUMN osm_id TEXT;")
                    conn.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
                }

                7 -> {
                    conn.execSQL("ALTER TABLE event RENAME TO event_old;")
                    // Inlined historical schema rather than event.CREATE:
                    // migrations must keep producing the schema of their own
                    // version, and the shared CREATE has since grown columns
                    // (updated_at) added by later migrations.
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
                        SELECT id, area_id, lat, lon, name, website, starts_at, ends_at
                        FROM event_old;
                        """
                    )
                    conn.execSQL("DROP TABLE event_old;")
                }

                8 -> {
                    conn.execSQL(org.btcmap.db.table.preference.CREATE)
                }

                9 -> {
                    conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
                    conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_UPDATED_AT)
                }

                10 -> {
                    // Recreate the place index with the id tie-break added to
                    // the sort key; the version 9 index omitted it, forcing
                    // SQLite to sort each place's comments in a temp B-tree.
                    conn.execSQL("DROP INDEX IF EXISTS comment_place_id_created_at;")
                    conn.execSQL(org.btcmap.db.table.comment.CREATE_INDEX_PLACE_ID_CREATED_AT)
                }

                11 -> {
                    // Event sync used to replace the whole table on every run;
                    // it is now incremental and needs updated_at as its cursor.
                    // Backfill the sentinel so the first delta re-reads every
                    // event and replaces the seeded value with the real one.
                    conn.execSQL(
                        "ALTER TABLE event ADD COLUMN " +
                            "${org.btcmap.db.table.event.UPDATED_AT} TEXT NOT NULL " +
                            "DEFAULT '2000-01-01T00:00:00Z';"
                    )
                }

                else -> throw Exception("migration is missing")
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