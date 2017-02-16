package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.BitstampApi;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.util.ExchangeRatesFactory;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:35 PM
 */

public class BitstampExchangeRatesProvider extends JsonApiRatesProvider {
    private BitstampApi api;

    public BitstampExchangeRatesProvider() {
        api = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(BitstampApi.class);
    }

    @Override
    public ExchangeRate getExchangeRate(String currency, String baseCurrency) throws IOException {
        if ("BTC".equalsIgnoreCase(currency) && "USD".equalsIgnoreCase(baseCurrency)) {
            return ExchangeRatesFactory.newExchangeRate(
                    currency,
                    baseCurrency,
                    api.getTicker().execute().body().getLast()
            );
        }

        throw new UnsupportedOperationException("Unsupported currency pair");
    }
}