package com.bubelov.coins.database;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:12
 */

public interface Tables {
    interface Merchants extends BaseColumns {
        String TABLE_NAME = "merchants";

        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String NAME = "name";
        String DESCRIPTION = "description";
        String PHONE = "phone";
        String WEBSITE = "website";
        String AMENITY = "amenity";
    }

    interface Currencies extends BaseColumns {
        String TABLE_NAME = "currencies";

        String NAME = "name";
        String CODE = "code";
        String SHOW_ON_MAP = "show_on_map";
    }

    interface CurrenciesMerchants extends BaseColumns {
        String TABLE_NAME = "currencies_merchants";

        String CURRENCY_ID = "currency_id";
        String MERCHANT_ID = "merchant_id";
    }
}