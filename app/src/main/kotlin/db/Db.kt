package db

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import java.time.ZonedDateTime

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "btcmap-v8.db",
    )

    return database(driver)
}

fun database(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        confAdapter = confAdapter(),
    )
}

private fun confAdapter(): Conf.Adapter {
    return Conf.Adapter(
        lastSyncDateAdapter = object : ColumnAdapter<ZonedDateTime, String> {
            override fun decode(databaseValue: String) = ZonedDateTime.parse(databaseValue)
            override fun encode(value: ZonedDateTime) = value.toString()
        },
    )
}