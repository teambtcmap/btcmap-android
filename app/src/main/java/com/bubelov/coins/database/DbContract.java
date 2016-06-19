package com.bubelov.coins.database;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:12
 */

public interface DbContract {
    interface Merchants extends BaseColumns {
        String TABLE_NAME = "merchants";

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

        String NAME = "name";
        String CODE = "code";
        String CRYPTO = "crypto";
        String SHOW_ON_MAP = "show_on_map";
    }

    interface CurrenciesMerchants extends BaseColumns {
        String TABLE_NAME = "currencies_merchants";

        String CURRENCY_ID = "currency_id";
        String MERCHANT_ID = "merchant_id";
    }

    interface ExchangeRates extends BaseColumns {
        String TABLE_NAME = "exchange_rates";

        String CURRENCY = "currency";
        String BASE_CURRENCY = "base_currency";
        String VALUE = "value";
    }
}