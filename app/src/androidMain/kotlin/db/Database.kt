package db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import androidx.sqlite.use
import area.AreaQueries
import conf.ConfQueries
import element.ElementQueries
import event.EventQueries
import kotlinx.coroutines.flow.MutableStateFlow
import reports.ReportQueries
import user.UserQueries
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock

class Database(path: String) {

    private val conn: SQLiteConnection =
        BundledSQLiteDriver().open(
            path,
            SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_FULLMUTEX
        )
            .apply {
                execSQL("PRAGMA journal_mode=WAL")
                execSQL("PRAGMA synchronous=NORMAL")

                val version = prepare("SELECT user_version FROM pragma_user_version").use {
                    if (it.step()) {
                        it.getInt(0)
                    } else {
                        0
                    }
                }

                if (version == 0) {
                    execSQL(ElementQueries.CREATE_TABLE)
                    execSQL(EventQueries.CREATE_TABLE)
                    execSQL(ReportQueries.CREATE_TABLE)
                    execSQL(UserQueries.CREATE_TABLE)
                    execSQL(AreaQueries.CREATE_TABLE)
                    execSQL(ConfQueries.CREATE_TABLE)
                    execSQL("PRAGMA user_version=1")
                }
            }

    private val connLock = ReentrantLock()

    fun <T> withConn(action: (conn: SQLiteConnection) -> T): T {
        connLock.lock()
        return try {
            action(conn)
        } finally {
            connLock.unlock()
        }
    }

    fun <T> transaction(action: (conn: SQLiteConnection) -> T) {
        return withConn { conn ->
            conn.execSQL("BEGIN TRANSACTION")

            try {
                action(conn)
                conn.execSQL("END TRANSACTION")
            } catch (t: Throwable) {
                conn.execSQL("ROLLBACK TRANSACTION")
            }
        }
    }
}

val elementsUpdatedAt = MutableStateFlow(LocalDateTime.now())