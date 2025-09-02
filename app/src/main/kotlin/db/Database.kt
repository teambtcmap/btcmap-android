package db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_FULLMUTEX
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import element.ElementQueries
import element_comment.ElementCommentQueries
import kotlinx.coroutines.flow.MutableStateFlow
import user.UserQueries
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock

class Database(path: String) {
    val conn: SQLiteConnection =
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
                    execSQL(ElementCommentQueries.CREATE_TABLE)
                    execSQL(ElementQueries.CREATE_TABLE)
                    execSQL(UserQueries.CREATE_TABLE)
                    execSQL(event.SCHEMA)
                    execSQL("PRAGMA user_version=1")
                }
                
                execSQL(ElementCommentQueries.CREATE_INDICES)
            }
}

val transactionLock = ReentrantLock()

fun <T> SQLiteConnection.transaction(action: (conn: SQLiteConnection) -> T) {
    transactionLock.lock()
    this.execSQL("BEGIN TRANSACTION")
    try {
        action(this)
        this.execSQL("END TRANSACTION")
    } catch (t: Throwable) {
        this.execSQL("ROLLBACK TRANSACTION")
    } finally {
        transactionLock.unlock()
    }
}

val elementsUpdatedAt = MutableStateFlow(LocalDateTime.now())