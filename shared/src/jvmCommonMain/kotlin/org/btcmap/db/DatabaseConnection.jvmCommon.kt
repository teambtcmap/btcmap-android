package org.btcmap.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import org.btcmap.platform.PlatformLock

internal actual suspend fun openDatabaseConnection(
    driver: SQLiteDriver,
    path: String,
): SQLiteConnection = LockingSQLiteConnection(driver.open(path))

internal actual suspend fun <T> withTransaction(conn: SQLiteConnection, block: suspend () -> T): T {
    if (conn is LockingSQLiteConnection) return conn.transaction(block)

    conn.execSQL("BEGIN TRANSACTION;")
    try {
        val result = block()
        conn.execSQL("COMMIT;")
        return result
    } catch (e: Throwable) {
        try {
            conn.execSQL("ROLLBACK;")
        } catch (_: Throwable) {
            // Ignored: the original exception is already on its way out.
        }
        throw e
    }
}

/**
 * Serializes every statement executed on a [SQLiteConnection].
 *
 * The app shares one connection between the UI's reads and the background sync,
 * seed and session writers, all of which run on the multi-threaded
 * [kotlinx.coroutines.Dispatchers.IO]. Android renders query results into a
 * `CursorWindow` that is refilled lazily as the cursor advances; a write
 * committed on another thread between two refills changes the window's row
 * count underneath the cursor, so the next `step()`/`get*()` throws. Holding
 * [lock] for the whole lifetime of a prepared statement guarantees a read
 * cursor can never be interleaved with a concurrent write. The lock is
 * reentrant, so a statement prepared inside [transaction] does not deadlock.
 */
internal class LockingSQLiteConnection(
    private val delegate: SQLiteConnection,
) : SQLiteConnection by delegate {

    private val lock = PlatformLock()

    override fun prepare(sql: String): SQLiteStatement {
        lock.lock()
        try {
            return LockingSQLiteStatement(delegate.prepare(sql), lock)
        } catch (t: Throwable) {
            lock.unlock()
            throw t
        }
    }

    override fun close() {
        lock.lock()
        try {
            delegate.close()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Runs [block] as a single atomic unit that no other statement can
     * interleave with. Nested [prepare] calls re-enter [lock] and therefore join
     * the same transaction instead of deadlocking.
     */
    suspend fun <T> transaction(block: suspend () -> T): T {
        lock.lock()
        try {
            delegate.execSQL("BEGIN TRANSACTION;")
            try {
                val result = block()
                delegate.execSQL("COMMIT;")
                return result
            } catch (e: Throwable) {
                try {
                    delegate.execSQL("ROLLBACK;")
                } catch (_: Throwable) {
                    // Ignored: the original exception is already on its way out.
                }
                throw e
            }
        } finally {
            lock.unlock()
        }
    }
}

private class LockingSQLiteStatement(
    private val delegate: SQLiteStatement,
    private val lock: PlatformLock,
) : SQLiteStatement by delegate {

    private var closed = false

    override fun close() {
        try {
            delegate.close()
        } finally {
            if (!closed) {
                closed = true
                lock.unlock()
            }
        }
    }
}
