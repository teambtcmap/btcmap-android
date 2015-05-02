package com.bubelov.coins.database;

import android.provider.BaseColumns;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:12
 */

public final class Tables {
    private Tables() {
        // Nothing to do here
    }

    public static abstract class Merchants implements BaseColumns {
        public static final String TABLE_NAME = "merchants";

        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String PHONE = "phone";
        public static final String WEBSITE = "website";
        public static final String AMENITY = "amenity";
    }

    public static abstract class Currencies implements BaseColumns {
        public static final String TABLE_NAME = "currencies";

        public static final String NAME = "name";
        public static final String CODE = "code";
        public static final String SHOW_ON_MAP = "show_on_map";
    }

    public static abstract class CurrenciesMerchants implements BaseColumns {
        public static final String TABLE_NAME = "currencies_merchants";

        public static final String CURRENCY_ID = "currency_id";
        public static final String MERCHANT_ID = "merchant_id";
    }
}