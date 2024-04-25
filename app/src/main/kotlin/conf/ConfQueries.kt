package conf

import androidx.sqlite.db.transaction
import db.getBoolean
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
                        viewport_west_lon,
                        show_atms,
                        show_osm_attribution,
                        show_sync_summary,
                        show_all_new_elements
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """,
                    arrayOf(
                        conf.lastSyncDate ?: "",
                        conf.viewportNorthLat,
                        conf.viewportEastLon,
                        conf.viewportSouthLat,
                        conf.viewportWestLon,
                        if (conf.showAtms) 1 else 0,
                        if (conf.showOsmAttribution) 1 else 0,
                        if (conf.showSyncSummary) 1 else 0,
                        if (conf.showAllNewElements) 1 else 0,
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
                    viewport_west_lon,
                    show_atms,
                    show_osm_attribution,
                    show_sync_summary,
                    show_all_new_elements
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
                showAtms = cursor.getBoolean(5),
                showOsmAttribution = cursor.getBoolean(6),
                showSyncSummary = cursor.getBoolean(7),
                showAllNewElements = cursor.getBoolean(8),
            )
        }
    }
}