package conf

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.use
import db.getZonedDateTimeOrNull
import db.transaction

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
                show_sync_summary INTEGER NOT NULL,
                show_all_new_elements INTEGER NOT NULL
            );
            """
    }

    fun insertOrReplace(conf: Conf) {
        conn.transaction { conn ->
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
                    show_sync_summary,
                    show_all_new_elements
                )
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8);
                """
            ).use {
                val lastSyncDate = conf.lastSyncDate
                if (lastSyncDate != null) {
                    it.bindText(1, lastSyncDate.toString())
                } else {
                    it.bindNull(1)
                }
                it.bindDouble(2, conf.viewportNorthLat)
                it.bindDouble(3, conf.viewportEastLon)
                it.bindDouble(4, conf.viewportSouthLat)
                it.bindDouble(5, conf.viewportWestLon)
                it.bindLong(6, if (conf.showAtms) 1 else 0)
                it.bindLong(7, if (conf.showSyncSummary) 1 else 0)
                it.bindLong(8, if (conf.notifyOfNewElementsNearby) 1 else 0)
                it.step()
            }
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
                    show_sync_summary,
                    show_all_new_elements
                FROM conf
                """
        ).use {
            if (it.step()) {
                Conf(
                    lastSyncDate = it.getZonedDateTimeOrNull(0),
                    viewportNorthLat = it.getDouble(1),
                    viewportEastLon = it.getDouble(2),
                    viewportSouthLat = it.getDouble(3),
                    viewportWestLon = it.getDouble(4),
                    showAtms = it.getBoolean(5),
                    showSyncSummary = it.getBoolean(6),
                    notifyOfNewElementsNearby = it.getBoolean(7),
                )
            } else {
                null
            }
        }
    }
}