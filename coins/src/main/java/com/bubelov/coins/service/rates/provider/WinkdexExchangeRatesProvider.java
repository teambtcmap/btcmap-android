package com.bubelov.coins.service.rates.provider;

import com.bubelov.coins.api.external.WinkDexApi;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.util.ExchangeRatesFactory;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 7:00 PM
 */

public class WinkdexExchangeRatesProvider extends JsonApiRatesProvider {
    private WinkDexApi api;

    public WinkdexExchangeRatesProvider() {
        api = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(WinkDexApi.class);
    }

    @Override
    public ExchangeRate getExchangeRate(Currency sourceCurrency, Currency targetCurrency) throws IOException {
        if (sourceCurrency.getCode().equals("BTC") && targetCurrency.getCode().equals("USD")) {
            return ExchangeRatesFactory.newExchangeRate(
                    sourceCurrency,
                    targetCurrency,
                    (float) api.getPrice().execute().body().getPrice() / 100.0f
            );
        }

        throw new UnsupportedOperationException("Unsupported currency pair");
    }
}