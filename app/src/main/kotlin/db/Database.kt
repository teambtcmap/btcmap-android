package db

import android.content.Context
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import org.json.JSONObject

fun database(context: Context): Database {
    return Database(
        driver = AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = "btcmap-v2.db",
        ),
        PlaceAdapter = placeAdapter(),
    )
}

private fun placeAdapter(): Place.Adapter {
    return Place.Adapter(
        tagsAdapter = object : ColumnAdapter<JSONObject, String> {
            override fun decode(databaseValue: String) =
                if (databaseValue.isEmpty()) {
                    JSONObject()
                } else {
                    JSONObject(databaseValue)
                }

            override fun encode(value: JSONObject) = value.toString()
        }
    )
}