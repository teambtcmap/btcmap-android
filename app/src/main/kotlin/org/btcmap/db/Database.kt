package org.btcmap.db

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import org.btcmap.db.table.CommentQueries
import org.btcmap.db.table.CommentSchema
import org.btcmap.db.table.PlaceQueries
import org.btcmap.db.table.PlaceSchema
import org.btcmap.db.table.UserQueries
import org.btcmap.db.table.UserSchema
import org.btcmap.db.table.event.EventQueries

class Database(driver: SQLiteDriver, val path: String) {
    companion object {
        private const val VERSION = 5
    }

    val conn = driver.open(path)

    val place = PlaceQueries(conn)
    val comment = CommentQueries(conn)
    val event = EventQueries(conn)
    val user = UserQueries(conn)

    init {
        migrate()
    }

    private fun migrate() {
        val stmt = conn.prepare("SELECT user_version FROM pragma_user_version;")
        var version = if (stmt.step()) stmt.getInt(0) else 0

        if (version == 0) {
            conn.execSQL(PlaceSchema.toString())
            conn.execSQL(org.btcmap.db.table.event.CREATE)
            conn.execSQL(CommentSchema.toString())
            conn.execSQL(UserSchema.toString())
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
                    conn.execSQL(UserSchema.toString())
                }

                4 -> {
                    conn.execSQL("ALTER TABLE event ADD COLUMN area_id INTEGER;")
                    conn.execSQL("ALTER TABLE event ADD COLUMN cron_schedule TEXT;")
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