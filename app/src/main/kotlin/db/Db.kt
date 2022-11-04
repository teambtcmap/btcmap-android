package db

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "btcmap-v27.db",
        factory = RequerySQLiteOpenHelperFactory(),
    )

    return database(driver)
}

fun database(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        confAdapter = confAdapter(),
        elementAdapter = elementAdapter(),
        areaAdapter = areaAdapter(),
        reportAdapter = reportAdapter(),
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

private fun elementAdapter(): Element.Adapter {
    return Element.Adapter(
        osm_jsonAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String): JsonObject =
                Json.decodeFromString(databaseValue)

            override fun encode(value: JsonObject) = value.toString()
        },
        tagsAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String): JsonObject =
                Json.decodeFromString(databaseValue)

            override fun encode(value: JsonObject) = value.toString()
        },
    )
}

private fun areaAdapter(): Area.Adapter {
    return Area.Adapter(
        tagsAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String): JsonObject =
                Json.decodeFromString(databaseValue)

            override fun encode(value: JsonObject) = value.toString()
        },
    )
}

private fun reportAdapter(): Report.Adapter {
    return Report.Adapter(
        tagsAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String): JsonObject =
                Json.decodeFromString(databaseValue)

            override fun encode(value: JsonObject) = value.toString()
        },
    )
}