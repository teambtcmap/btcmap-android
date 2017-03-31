package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.BitstampApi;
import com.bubelov.coins.dagger.Injector;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Igor Bubelov
 */

public class Bitstamp implements CryptoExchange {
    private BitstampApi api;

    public Bitstamp() {
        api = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(GsonConverterFactory.create(Injector.INSTANCE.mainComponent().gson()))
                .build()
                .create(BitstampApi.class);
    }

    @Override
    public double getCurrentRate() throws IOException {
        return api.getTicker().execute().body().getLast();
    }
}