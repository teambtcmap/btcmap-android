package db

import android.content.Context
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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

val elementsUpdatedAt = MutableStateFlow(LocalDateTime.now())

private const val DB_FILE_NAME = "btcmap-2024-05-15.db"

fun openDbConnection(context: Context): SQLiteConnection {
    return BundledSQLiteDriver().open(context.getDatabasePath(DB_FILE_NAME).absolutePath)
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
}