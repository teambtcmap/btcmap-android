package log

import io.requery.android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.time.OffsetDateTime

class LogRecordQueries(private val db: SQLiteOpenHelper) {

    fun insert(tags: JSONObject) {
        db.writableDatabase.execSQL(
            "INSERT INTO log(tags, created_at) VALUES (?, ?);",
            arrayOf(tags, OffsetDateTime.now()),
        )
    }
}