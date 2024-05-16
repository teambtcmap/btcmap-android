package conf

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import db.getZonedDateTime

class ConfQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE conf (
                last_sync_date TEXT NOT NULL,
                viewport_north_lat REAL NOT NULL,
                viewport_east_lon REAL NOT NULL,
                viewport_south_lat REAL NOT NULL,
                viewport_west_lon REAL NOT NULL,
                show_atms INTEGER NOT NULL,
                show_osm_attribution INTEGER NOT NULL,
                show_sync_summary INTEGER NOT NULL,
                show_all_new_elements INTEGER NOT NULL
            );
            """
    }

    fun insertOrReplace(conf: Conf) {
        conn.execSQL("BEGIN IMMEDIATE TRANSACTION")

        try {
            conn.execSQL("DELETE FROM conf")

            conn.prepare(
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
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
            """
            ).use {
                it.bindText(1, conf.lastSyncDate?.toString() ?: "")
                it.bindDouble(2, conf.viewportNorthLat)
                it.bindDouble(3, conf.viewportEastLon)
                it.bindDouble(4, conf.viewportSouthLat)
                it.bindDouble(5, conf.viewportWestLon)
                it.bindLong(6, if (conf.showAtms) 1 else 0)
                it.bindLong(7, if (conf.showOsmAttribution) 1 else 0)
                it.bindLong(8, if (conf.showSyncSummary) 1 else 0)
                it.bindLong(9, if (conf.showAllNewElements) 1 else 0)
            }

            conn.execSQL("END TRANSACTION")
        } catch (t: Throwable) {
            conn.execSQL("ROLLBACK TRANSACTION")
        }
    }

    fun select(): Conf? {
        return conn.prepare(
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
            FROM conf
            """
        ).use {
            if (it.step()) {
                Conf(
                    lastSyncDate = it.getZonedDateTime(0),
                    viewportNorthLat = it.getDouble(1),
                    viewportEastLon = it.getDouble(2),
                    viewportSouthLat = it.getDouble(3),
                    viewportWestLon = it.getDouble(4),
                    showAtms = it.getBoolean(5),
                    showOsmAttribution = it.getBoolean(6),
                    showSyncSummary = it.getBoolean(7),
                    showAllNewElements = it.getBoolean(8),
                )
            } else {
                null
            }
        }
    }
}