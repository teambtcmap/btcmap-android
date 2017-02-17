package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.WinkDexApi;
import com.bubelov.coins.dagger.Injector;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Igor Bubelov
 */

public class Winkdex implements CryptoExchange {
    private WinkDexApi api;

    public Winkdex() {
        api = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addConverterFactory(GsonConverterFactory.create(Injector.INSTANCE.getAppComponent().provideGson()))
                .build()
                .create(WinkDexApi.class);
    }

    @Override
    public double getCurrentRate() throws IOException {
        return (float) api.getPrice().execute().body().getPrice() / 100.0f;
    }
}