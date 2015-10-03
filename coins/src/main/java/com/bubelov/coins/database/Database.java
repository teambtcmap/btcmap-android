package com.bubelov.coins.database;

import android.net.Uri;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:12
 */

public interface Database {
    String AUTHORITY = "com.bubelov.coins";
    String BASE_CONTENT_URI = String.format("content://%s", AUTHORITY);

    interface Merchants extends BaseColumns {
        String TABLE_NAME = "merchants";
        Uri CONTENT_URI = Uri.parse(String.format("%s/%s", BASE_CONTENT_URI, TABLE_NAME));

        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String NAME = "name";
        String DESCRIPTION = "description";
        String PHONE = "phone";
        String WEBSITE = "website";
        String AMENITY = "amenity";
        String OPENING_HOURS = "opening_hours";
        String ADDRESS = "address";
    }

    interface Currencies extends BaseColumns {
        String TABLE_NAME = "currencies";
        Uri CONTENT_URI = Uri.parse(String.format("%s/%s", BASE_CONTENT_URI, TABLE_NAME));

        String NAME = "name";
        String CODE = "code";
        String CRYPTO = "crypto";
        String SHOW_ON_MAP = "show_on_map";
    }

    interface CurrenciesMerchants extends BaseColumns {
        String TABLE_NAME = "currencies_merchants";
        Uri CONTENT_URI = Uri.parse(String.format("%s/%s", BASE_CONTENT_URI, TABLE_NAME));

        String CURRENCY_ID = "currency_id";
        String MERCHANT_ID = "merchant_id";
    }

    interface ExchangeRates extends BaseColumns {
        String TABLE_NAME = "exchange_rates";
        Uri CONTENT_URI = Uri.parse(String.format("%s/%s", BASE_CONTENT_URI, TABLE_NAME));

        String SOURCE_CURRENCY_ID = "source_currency_id";
        String TARGET_CURRENCY_ID = "target_currency_id";
        String VALUE = "value";
    }
}