package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.api.rates.BitcoinAverageApi;
import com.bubelov.coins.dagger.Injector;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Igor Bubelov
 */

public class BitcoinAverage implements CryptoExchange {
    private BitcoinAverageApi api;

    public BitcoinAverage() {
        api = new Retrofit.Builder()
                .baseUrl("https://api.bitcoinaverage.com/")
                .addConverterFactory(GsonConverterFactory.create(Injector.INSTANCE.getAppComponent().provideGson()))
                .build()
                .create(BitcoinAverageApi.class);
    }

    @Override
    public double getCurrentRate() throws IOException {
        return api.getUsdTicker().execute().body().getLast();
    }
}