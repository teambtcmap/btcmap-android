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
         * Schema version stamped onto databases created by this app.
         *
         * It is deliberately far above the single-digit user_version of the
         * throwaway database that used the `btcmap.db` name early in the
         * project's history, so the stale file can be told apart from ours by
         * the pragma alone (see [initialize]). Fresh databases are created at
         * this version; there is no migration chain by design.
         */
        const val VERSION = 101

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
        // [initialize] discards any pre-existing database that is not already at
        // [VERSION] or newer, so a version of 0 here means the file was just
        // created and the schema has to be created. A creation interrupted before
        // the version was stamped also reads as 0 and is deleted and retried on
        // the next open.
        if (readUserVersion(conn) == 0) {
            createSchema(conn)
        }
    }

    /**
     * Opens [path], first removing a database left behind by an older app that
     * used a different schema.
     *
     * The `btcmap.db` name was once used for a database with unrelated tables,
     * so rather than migrating it we discard any file whose user_version is
     * below [VERSION] (including a missing version, which reads as 0). Its
     * sidecar files are removed too. An unreadable file cannot be one of ours
     * either, so it is discarded as well.
     */
    private fun initialize(driver: SQLiteDriver, path: String): SQLiteConnection {
        if (path != MEMORY_PATH) {
            val file = File(path)
            if (file.exists() && readUserVersion(driver, path) < VERSION) {
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
