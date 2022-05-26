package db

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import org.json.JSONObject

fun database(context: Context): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = context,
        name = "btcmap-v2.db",
    )

    return database(driver)
}

fun database(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        PlaceAdapter = placeAdapter(),
    )
}

private fun placeAdapter(): Place.Adapter {
    return Place.Adapter(
        tagsAdapter = object : ColumnAdapter<JSONObject, String> {
            override fun decode(databaseValue: String) = JSONObject(databaseValue)
            override fun encode(value: JSONObject) = value.toString()
        }
    )
}