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
                .baseUrl("https://api.exchange.coinbase.com/")
                .addConverterFactory(GsonConverterFactory.create(Injector.INSTANCE.getAppComponent().provideGson()))
                .build()
                .create(CoinbaseApi.class);
    }

    @Override
    public double getCurrentRate() throws IOException {
        return api.getTicker().execute().body().getPrice();
    }
}