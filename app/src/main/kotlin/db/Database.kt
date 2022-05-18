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
        tagsAdapter = object : ColumnAdapter<Tags, String> {
            override fun decode(databaseValue: String) = Tags(databaseValue)
            override fun encode(value: Tags) = value.toString()
        }
    )
}