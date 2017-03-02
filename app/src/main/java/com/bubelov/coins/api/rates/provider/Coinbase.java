package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.CoinbaseApi;
import com.bubelov.coins.dagger.Injector;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Igor Bubelov
 */

public class Coinbase implements CryptoExchange {
    private CoinbaseApi api;

    public Coinbase() {
        api = new Retrofit.Builder()
                .baseUrl("https://api.coinbase.com/v2/")
                .addConverterFactory(GsonConverterFactory.create(Injector.INSTANCE.getCoreComponent().gson()))
                .build()
                .create(CoinbaseApi.class);
    }

    @Override
    public double getCurrentRate() throws IOException {
        return api.getExchangeRates().execute().body().data.rates.get("USD");
    }
}