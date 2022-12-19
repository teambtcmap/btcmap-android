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
                        dark_map,
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
                        conf.darkMap.toSqliteInt(),
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
                    dark_map,
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
                darkMap = cursor.getBoolean(2),
                viewportNorthLat = cursor.getDouble(3),
                viewportEastLon = cursor.getDouble(4),
                viewportSouthLat = cursor.getDouble(5),
                viewportWestLon = cursor.getDouble(6),
                showTags = cursor.getBoolean(7),
                osmLogin = cursor.getString(8),
                osmPassword = cursor.getString(9),
            )
        }
    }
}