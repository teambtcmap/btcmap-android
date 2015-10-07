package com.bubelov.coins.service.rates.provider;

import com.bubelov.coins.api.external.BitstampApi;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.util.ExchangeRatesFactory;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:35 PM
 */

public class BitstampExchangeRatesProvider extends JsonApiRatesProvider {
    private BitstampApi api;

    public BitstampExchangeRatesProvider() {
        api = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(BitstampApi.class);
    }

    @Override
    public ExchangeRate getExchangeRate(Currency sourceCurrency, Currency targetCurrency) throws IOException {
        if (sourceCurrency.getCode().equals("BTC") && targetCurrency.getCode().equals("USD")) {
            return ExchangeRatesFactory.newExchangeRate(
                    sourceCurrency,
                    targetCurrency,
                    api.getTicker().execute().body().getLast()
            );
        }

        throw new UnsupportedOperationException("Unsupported currency pair");
    }
}