package conf

import androidx.sqlite.db.transaction
import db.getZonedDateTime
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
                        viewport_north_lat,
                        viewport_east_lon,
                        viewport_south_lat,
                        viewport_west_lon
                    )
                    VALUES (?, ?, ?, ?, ?);
                    """,
                    arrayOf(
                        conf.lastSyncDate ?: "",
                        conf.viewportNorthLat,
                        conf.viewportEastLon,
                        conf.viewportSouthLat,
                        conf.viewportWestLon,
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
                    viewport_north_lat,
                    viewport_east_lon,
                    viewport_south_lat,
                    viewport_west_lon
                FROM conf;
                """,
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            Conf(
                lastSyncDate = cursor.getZonedDateTime(0),
                viewportNorthLat = cursor.getDouble(1),
                viewportEastLon = cursor.getDouble(2),
                viewportSouthLat = cursor.getDouble(3),
                viewportWestLon = cursor.getDouble(4),
            )
        }
    }
}