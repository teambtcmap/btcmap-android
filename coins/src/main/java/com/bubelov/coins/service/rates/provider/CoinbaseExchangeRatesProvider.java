package com.bubelov.coins.service.rates.provider;

import com.bubelov.coins.api.external.CoinbaseApi;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;
import com.bubelov.coins.util.ExchangeRatesFactory;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:56 PM
 */

public class CoinbaseExchangeRatesProvider extends JsonApiRatesProvider {
    private CoinbaseApi api;

    public CoinbaseExchangeRatesProvider() {
        api = new Retrofit.Builder()
                .baseUrl("https://api.exchange.coinbase.com/")
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(CoinbaseApi.class);
    }

    @Override
    public ExchangeRate getExchangeRate(Currency sourceCurrency, Currency targetCurrency) throws IOException {
        if (sourceCurrency.getCode().equals("BTC") && targetCurrency.getCode().equals("USD")) {
            return ExchangeRatesFactory.newExchangeRate(
                    sourceCurrency,
                    targetCurrency,
                    api.getTicker().execute().body().getPrice()
            );
        }

        throw new UnsupportedOperationException("Unsupported currency pair");
    }
}
