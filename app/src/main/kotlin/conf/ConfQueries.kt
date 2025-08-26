package conf

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import db.transaction

class ConfQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE conf (
                viewport_north_lat REAL NOT NULL,
                viewport_east_lon REAL NOT NULL,
                viewport_south_lat REAL NOT NULL,
                viewport_west_lon REAL NOT NULL,
                show_atms INTEGER NOT NULL,
                map_style TEXT NOT NULL
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
                    viewport_north_lat,
                    viewport_east_lon,
                    viewport_south_lat,
                    viewport_west_lon,
                    show_atms,
                    map_style
                )
                VALUES (?1, ?2, ?3, ?4, ?5, ?6);
                """
            ).use {
                it.bindDouble(1, conf.viewportNorthLat)
                it.bindDouble(2, conf.viewportEastLon)
                it.bindDouble(3, conf.viewportSouthLat)
                it.bindDouble(4, conf.viewportWestLon)
                it.bindLong(5, if (conf.showAtms) 1 else 0)
                it.bindText(6, conf.mapStyle.toString())
                it.step()
            }
        }
    }

    fun select(): Conf? {
        return conn.prepare(
            """
                SELECT
                    viewport_north_lat,
                    viewport_east_lon,
                    viewport_south_lat,
                    viewport_west_lon,
                    show_atms,
                    map_style
                FROM conf
                """
        ).use {
            if (it.step()) {
                Conf(
                    viewportNorthLat = it.getDouble(0),
                    viewportEastLon = it.getDouble(1),
                    viewportSouthLat = it.getDouble(2),
                    viewportWestLon = it.getDouble(3),
                    showAtms = it.getBoolean(4),
                    mapStyle = MapStyle.valueOf(it.getText(5))
                )
            } else {
                null
            }
        }
    }
}