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
const val UPDATED_AT = "updated_at"
const val DELETED_AT = "deleted_at"

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
        $UPDATED_AT TEXT NOT NULL,
        $DELETED_AT TEXT
    );
 """
