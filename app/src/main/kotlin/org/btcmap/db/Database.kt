package org.btcmap.db

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import org.btcmap.db.table.comment.CommentQueries
import org.btcmap.db.table.comment.CommentSchema
import org.btcmap.db.table.event.EventQueries
import org.btcmap.db.table.event.EventSchema
import org.btcmap.db.table.place.PlaceQueries
import org.btcmap.db.table.place.PlaceSchema

class Database(driver: SQLiteDriver, val path: String) {

    val conn = driver.open(path)

    val place = PlaceQueries(conn)
    val comment = CommentQueries(conn)
    val event = EventQueries(conn)

    init {
        migrate()
    }

    private fun migrate() {
        val stmt = conn.prepare("SELECT user_version FROM pragma_user_version;")
        var version = if (stmt.step()) stmt.getInt(0) else 0

        if (version == 0) {
            conn.execSQL(PlaceSchema.toString())
            conn.execSQL(EventSchema.toString())
            conn.execSQL(CommentSchema.toString())
            conn.execSQL("PRAGMA user_version=1;")
            return
        }

        if (version == 1) {
            conn.execSQL("ALTER TABLE place ADD COLUMN localized_name TEXT;")
            conn.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
            conn.execSQL("PRAGMA user_version=2;")
            version = 2
        }

        if (version == 2) {
            conn.execSQL("ALTER TABLE place ADD COLUMN localized_opening_hours TEXT;")
            conn.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
            conn.execSQL("PRAGMA user_version=3;")
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