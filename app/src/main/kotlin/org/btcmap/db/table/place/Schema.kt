package org.btcmap.db.table.place

const val TABLE = "place"

const val ID = "id"
const val UPDATED_AT = "updated_at"
const val LAT = "lat"
const val LON = "lon"
const val ICON = "icon"
const val NAME = "name"
const val LOCALIZED_NAME = "localized_name"
const val VERIFIED_AT = "verified_at"
const val ADDRESS = "address"
const val OPENING_HOURS = "opening_hours"
const val LOCALIZED_OPENING_HOURS = "localized_opening_hours"
const val PHONE = "phone"
const val WEBSITE = "website"
const val EMAIL = "email"
const val TWITTER = "twitter"
const val FACEBOOK = "facebook"
const val INSTAGRAM = "instagram"
const val LINE = "line"
const val REQUIRED_APP_URL = "required_app_url"
const val BOOSTED_UNTIL = "boosted_until"
const val COMMENTS = "comments"
const val TELEGRAM = "telegram"
const val OSM_ID = "osm_id"
const val DELETED_AT = "deleted_at"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $UPDATED_AT TEXT NOT NULL,
        $LAT REAL NOT NULL,
        $LON REAL NOT NULL,
        $ICON TEXT NOT NULL,
        $NAME TEXT,
        $LOCALIZED_NAME TEXT,
        $VERIFIED_AT TEXT,
        $ADDRESS TEXT,
        $OPENING_HOURS TEXT,
        $LOCALIZED_OPENING_HOURS TEXT,
        $PHONE TEXT,
        $WEBSITE TEXT,
        $EMAIL TEXT,
        $TWITTER TEXT,
        $FACEBOOK TEXT,
        $INSTAGRAM TEXT,
        $LINE TEXT,
        $REQUIRED_APP_URL TEXT,
        $BOOSTED_UNTIL TEXT,
        $COMMENTS INTEGER,
        $TELEGRAM TEXT,
        $OSM_ID TEXT,
        $DELETED_AT TEXT
    );
"""

// Serves selectMaxUpdatedAt: the expression index lets SQLite read the newest
// row directly instead of scanning and sorting the whole table, mirroring
// comment_updated_at.
const val CREATE_INDEX_UPDATED_AT =
    "CREATE INDEX place_updated_at ON $TABLE(julianday($UPDATED_AT));"

// Serves selectByOsmId and selectByOsmIds, which resolve the places behind a
// list of issues.
const val CREATE_INDEX_OSM_ID = "CREATE INDEX place_osm_id ON $TABLE($OSM_ID);"

// Serves the bounding-box reads: the leading latitude range keeps SQLite from
// scanning the whole table, and a descending scan also satisfies ORDER BY lat.
const val CREATE_INDEX_BOUNDS = "CREATE INDEX place_bounds ON $TABLE($LAT, $LON);"