package org.btcmap.db.table.event

const val TABLE = "event"

const val ID = "id"
const val AREA_ID = "area_id"
const val LAT = "lat"
const val LON = "lon"
const val NAME = "name"
const val WEBSITE = "website"
const val STARTS_AT = "starts_at"
const val ENDS_AT = "ends_at"
const val CRON_SCHEDULE = "cron_schedule"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $AREA_ID INTEGER,
        $LAT REAL NOT NULL,
        $LON REAL NOT NULL,
        $NAME TEXT NOT NULL,
        $WEBSITE TEXT NOT NULL,
        $STARTS_AT TEXT NOT NULL,
        $ENDS_AT TEXT,
        $CRON_SCHEDULE TEXT
    );
 """