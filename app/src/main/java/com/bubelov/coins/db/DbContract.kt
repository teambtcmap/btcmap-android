package com.bubelov.coins.db

/**
 * @author Igor Bubelov
 */

interface DbContract {
    object Places {
        val TABLE_NAME = "places"

        val _ID = "_id"
        val NAME = "name"
        val LATITUDE = "latitude"
        val LONGITUDE = "longitude"
        val CATEGORY = "category_id"
        val DESCRIPTION = "description"
        val CURRENCIES = "currencies"
        val OPENED_CLAIMS = "opened_claims"
        val CLOSED_CLAIMS = "closed_claims"
        val PHONE = "phone"
        val WEBSITE = "website"
        val OPENING_HOURS = "opening_hours"
        val VISIBLE = "visible"

        val _UPDATED_AT = "_updated_at"
    }
}