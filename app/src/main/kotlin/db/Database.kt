package db

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver

fun database(context: Context): Database {
    return Database(
        driver = AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "btcmap.db",
        ),
        PlaceAdapter = placeAdapter(),
    )
}

private fun placeAdapter(): Place.Adapter {
    return Place.Adapter(
        tagsAdapter = object : ColumnAdapter<JsonObject, String> {
            override fun decode(databaseValue: String) =
                if (databaseValue.isEmpty()) {
                    JsonObject()
                } else {
                    Gson().fromJson(databaseValue, JsonObject::class.java)
                }

            override fun encode(value: JsonObject) = value.toString()
        }
    )
}