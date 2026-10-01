package org.btcmap.db.table.area

const val TABLE = "area"

const val ID = "id"
const val NAME = "name"
const val TYPE = "type"
const val URL_ALIAS = "url_alias"
const val ICON = "icon"
const val ICON_WIDE = "icon_wide"
const val WEBSITE_URL = "website_url"
const val DESCRIPTION = "description"
const val BBOX_WEST = "bbox_west"
const val BBOX_SOUTH = "bbox_south"
const val BBOX_EAST = "bbox_east"
const val BBOX_NORTH = "bbox_north"
const val GEO_JSON = "geo_json"
const val UPDATED_AT = "updated_at"
const val DELETED_AT = "deleted_at"

// Per-language `name:<lang>` / `description:<lang>` maps stored as JSON text,
// keyed by a two-letter language code, or null when the area has no
// translation. See i18n/Area.kt for how the locale is resolved.
const val LOCALIZED_NAME = "localized_name"
const val LOCALIZED_DESCRIPTION = "localized_description"

const val CREATE = """
    CREATE TABLE $TABLE (
        $ID INTEGER PRIMARY KEY NOT NULL,
        $NAME TEXT NOT NULL,
        $TYPE TEXT NOT NULL,
        $URL_ALIAS TEXT NOT NULL,
        $ICON TEXT,
        $ICON_WIDE TEXT,
        $WEBSITE_URL TEXT NOT NULL,
        $DESCRIPTION TEXT,
        $BBOX_WEST REAL,
        $BBOX_SOUTH REAL,
        $BBOX_EAST REAL,
        $BBOX_NORTH REAL,
        $GEO_JSON TEXT,
        $UPDATED_AT TEXT NOT NULL,
        $DELETED_AT TEXT,
        $LOCALIZED_NAME TEXT,
        $LOCALIZED_DESCRIPTION TEXT
    );
 """

// See place.CREATE_INDEX_UPDATED_AT: same cursor read, same expression index.
const val CREATE_INDEX_UPDATED_AT =
    "CREATE INDEX area_updated_at ON $TABLE(julianday($UPDATED_AT));"
