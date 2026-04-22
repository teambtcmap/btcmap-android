package org.btcmap.db.table.place

const val TABLE = "place"

const val ID = "id"
const val BUNDLED = "bundled"
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

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $BUNDLED INTEGER NOT NULL,
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
        $TELEGRAM TEXT
    );
"""