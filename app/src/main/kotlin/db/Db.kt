package db

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import java.time.ZonedDateTime

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "btcmap-v18.db",
        factory = RequerySQLiteOpenHelperFactory(),
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