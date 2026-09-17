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
        private const val VERSION = 9
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
                    conn.execSQL(org.btcmap.db.table.event.CREATE)
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

                else -> throw Exception("migration is missing")
            }

            conn.execSQL("PRAGMA user_version=${++version};")
        }
    }

    fun transaction(block: () -> Unit) {
        conn.execSQL("BEGIN TRANSACTION;")
        try {
            block()
            conn.execSQL("COMMIT;")
        } catch (e: Exception) {
            conn.execSQL("ROLLBACK;")
            throw e
        }
    }
}