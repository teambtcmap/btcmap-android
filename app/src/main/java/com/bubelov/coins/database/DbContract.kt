package com.bubelov.coins.database

/**
 * @author Igor Bubelov
 */

interface DbContract {
    object Places {
        val TABLE_NAME = "places"

        val _ID = "_id"
        val LATITUDE = "latitude"
        val LONGITUDE = "longitude"
        val NAME = "name"
        val DESCRIPTION = "description"
        val PHONE = "phone"
        val WEBSITE = "website"
        val CATEGORY_ID = "category_id"
        val OPENING_HOURS = "opening_hours"
        val VISIBLE = "visible"

        val _UPDATED_AT = "_updated_at"
    }

    object PlaceCategories {
        val TABLE_NAME = "place_categories"

        val _ID = "_id"
        val NAME = "name"
    }

    object Currencies {
        val TABLE_NAME = "currencies"

        val _ID = "_id"
        val NAME = "name"
        val CODE = "code"
        val CRYPTO = "crypto"
    }
}