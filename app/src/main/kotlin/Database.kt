import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import app.App
import element.ElementQueries
import element_comment.ElementCommentQueries
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZonedDateTime

private lateinit var path: String

fun App.initDatabase() {
    path = getDatabasePath("btcmap-2025-08-24.db").absolutePath

    val conn = BundledSQLiteDriver().open(
        path,
        SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE
    )

    if (conn.version() == 0) {
        conn.execSQL(ElementCommentQueries.CREATE_TABLE)
        conn.execSQL(ElementCommentQueries.CREATE_INDICES)
        conn.execSQL(ElementQueries.CREATE_TABLE)
        conn.execSQL(event.SCHEMA)
        conn.execSQL("PRAGMA user_version=1")
    }
}

fun conn(): SQLiteConnection {
    return BundledSQLiteDriver().open(path, SQLITE_OPEN_READWRITE).apply {
        execSQL("PRAGMA busy_timeout=30000")
    }
}

fun <T> SQLiteConnection.transaction(block: SQLiteConnection.() -> T): T {
    execSQL("BEGIN TRANSACTION")
    try {
        val result = block()
        execSQL("END TRANSACTION")
        return result
    } catch (t: Throwable) {
        execSQL("ROLLBACK TRANSACTION")
        throw t
    }
}

fun SQLiteStatement.getZonedDateTime(columnIndex: Int): ZonedDateTime {
    return ZonedDateTime.parse(getText(columnIndex))
}

fun SQLiteStatement.getZonedDateTimeOrNull(columnIndex: Int): ZonedDateTime? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getZonedDateTime(columnIndex)
    }
}

fun SQLiteStatement.getText(columnIndex: Int, defaultValue: String): String {
    return if (isNull(columnIndex)) {
        defaultValue
    } else {
        getText(columnIndex)
    }
}

fun SQLiteStatement.getTextOrNull(columnIndex: Int): String? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getText(columnIndex)
    }
}

fun SQLiteStatement.getHttpUrlOrNull(columnIndex: Int): HttpUrl? {
    return (getText(columnIndex, "")).toHttpUrlOrNull()
}

private fun SQLiteConnection.version(): Int {
    return prepare("SELECT user_version FROM pragma_user_version").use {
        if (it.step()) {
            it.getInt(0)
        } else {
            0
        }
    }
}