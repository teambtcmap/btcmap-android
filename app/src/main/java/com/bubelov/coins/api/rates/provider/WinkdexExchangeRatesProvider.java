package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.WinkDexApi;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.util.ExchangeRatesFactory;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 7:00 PM
 */

public class WinkdexExchangeRatesProvider extends JsonApiRatesProvider {
    private WinkDexApi api;

    public WinkdexExchangeRatesProvider() {
        api = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(WinkDexApi.class);
    }

    @Override
    public ExchangeRate getExchangeRate(String currency, String baseCurrency) throws IOException {
        if ("BTC".equalsIgnoreCase(currency) && "USD".equalsIgnoreCase(baseCurrency)) {
            return ExchangeRatesFactory.newExchangeRate(
                    currency,
                    baseCurrency,
                    (float) api.getPrice().execute().body().getPrice() / 100.0f
            );
        }

        throw new UnsupportedOperationException("Unsupported currency pair");
    }
}