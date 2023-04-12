package conf

import androidx.sqlite.db.transaction
import db.getBoolean
import db.getZonedDateTime
import db.toSqliteInt
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfQueries(private val db: SQLiteOpenHelper) {

    suspend fun insertOrReplace(conf: Conf) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.transaction {
                execSQL("DELETE FROM conf;")

                execSQL(
                    """
                    INSERT
                    INTO conf (
                        last_sync_date,
                        themed_pins,
                        viewport_north_lat,
                        viewport_east_lon,
                        viewport_south_lat,
                        viewport_west_lon,
                        show_tags,
                        osm_login,
                        osm_password
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    arrayOf(
                        conf.lastSyncDate ?: "",
                        conf.themedPins.toSqliteInt(),
                        conf.viewportNorthLat,
                        conf.viewportEastLon,
                        conf.viewportSouthLat,
                        conf.viewportWestLon,
                        conf.showTags.toSqliteInt(),
                        conf.osmLogin,
                        conf.osmPassword,
                    ),
                )
            }


        }
    }

    suspend fun select(): Conf? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    last_sync_date,
                    themed_pins,
                    viewport_north_lat,
                    viewport_east_lon,
                    viewport_south_lat,
                    viewport_west_lon,
                    show_tags,
                    osm_login,
                    osm_password
                FROM conf;
                """,
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            Conf(
                lastSyncDate = cursor.getZonedDateTime(0),
                themedPins = cursor.getBoolean(1),
                viewportNorthLat = cursor.getDouble(2),
                viewportEastLon = cursor.getDouble(3),
                viewportSouthLat = cursor.getDouble(4),
                viewportWestLon = cursor.getDouble(5),
                showTags = cursor.getBoolean(6),
                osmLogin = cursor.getString(7),
                osmPassword = cursor.getString(8),
            )
        }
    }
}