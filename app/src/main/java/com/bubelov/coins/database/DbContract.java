package com.bubelov.coins.database;

import android.provider.BaseColumns;

/**
 * @author Igor Bubelov
 */

public interface DbContract {
    interface Places extends BaseColumns {
        String TABLE_NAME = "places";

        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String NAME = "name";
        String DESCRIPTION = "description";
        String PHONE = "phone";
        String WEBSITE = "website";
        String AMENITY = "amenity";
        String OPENING_HOURS = "opening_hours";
        String ADDRESS = "address";
        String VISIBLE = "visible";
        String OPENED_CLAIMS = "opened_claims";
        String CLOSED_CLAIMS = "closed_claims";

        String _UPDATED_AT = "_updated_at";
    }

    interface Currencies extends BaseColumns {
        String TABLE_NAME = "currencies";

        String NAME = "name";
        String CODE = "code";
        String CRYPTO = "crypto";
    }

    interface CurrenciesPlaces extends BaseColumns {
        String TABLE_NAME = "currencies_places";

        String CURRENCY_ID = "currency_id";
        String PLACE_ID = "place_id";
    }
}