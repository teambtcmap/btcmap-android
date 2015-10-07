package com.bubelov.coins.service.rates.provider;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:36 PM
 */

public abstract class JsonApiRatesProvider implements ExchangeRatesProvider {
    protected Gson getGsonForApis() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}