package db

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.ZonedDateTime

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "btcmap-v6.db",
    )

    return database(driver)
}

fun database(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        ConfAdapter = confAdapter(),
        ElementAdapter = elementAdapter(),
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
        osm_dataAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String) =
                Json.parseToJsonElement(databaseValue).jsonObject

            override fun encode(value: JsonObject) = value.toString()
        },
    )
}