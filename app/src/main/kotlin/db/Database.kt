package db

import android.content.Context
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

val elementsUpdatedAt = MutableStateFlow(LocalDateTime.now())

fun persistentDatabase(context: Context): SQLiteOpenHelper {
    return Database(context, "btcmap-2023-06-01.db")
}

fun inMemoryDatabase(): SQLiteOpenHelper {
    return Database(null, null)
}

private class Database(context: Context?, name: String?) : SQLiteOpenHelper(
    context,
    name,
    null,
    1,
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE area (
                id TEXT NOT NULL PRIMARY KEY,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE conf (
                last_sync_date TEXT NOT NULL,
                viewport_north_lat REAL NOT NULL,
                viewport_east_lon REAL NOT NULL,
                viewport_south_lat REAL NOT NULL,
                viewport_west_lon REAL NOT NULL,
                show_atms INT NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE element (
                id TEXT NOT NULL PRIMARY KEY,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                osm_json TEXT NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE event (
                id INTEGER PRIMARY KEY,
                type TEXT NOT NULL,
                element_id TEXT NOT NULL,
                user_id INTEGER NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE report (
                area_id TEXT NOT NULL,
                date TEXT NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT NOT NULL
            );
            """
        )

        db.execSQL(
            """
            CREATE TABLE user (
                id INTEGER NOT NULL PRIMARY KEY,
                osm_json TEXT NOT NULL,
                tags TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                deleted_at TEXT NOT NULL
            );
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE area;")
        db.execSQL("DROP TABLE conf;")
        db.execSQL("DROP TABLE element;")
        db.execSQL("DROP TABLE event;")
        db.execSQL("DROP TABLE report;")
        db.execSQL("DROP TABLE user;")
        onCreate(db)
    }
}