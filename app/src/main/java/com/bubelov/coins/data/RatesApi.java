package com.bubelov.coins.data;

import com.bubelov.coins.data.api.rates.BitcoinAverageApi;
import com.bubelov.coins.data.api.rates.BitstampApi;
import com.bubelov.coins.data.api.rates.CoinbaseApi;
import com.bubelov.coins.data.api.rates.WinkDexApi;
import com.bubelov.coins.data.api.rates.model.ExchangeRate;
import com.google.gson.Gson;

import java.io.IOException;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 */

public class RatesApi {
    private BitcoinAverageApi bitcoinAverageApi;

    private BitstampApi bitstampApi;

    private CoinbaseApi coinbaseApi;

    private WinkDexApi winkDexApi;

    RatesApi(Gson gson) {
        bitcoinAverageApi = new Retrofit.Builder()
                .baseUrl("https://api.bitcoinaverage.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(BitcoinAverageApi.class);

        bitstampApi = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(BitstampApi.class);

        coinbaseApi = new Retrofit.Builder()
                .baseUrl("https://api.coinbase.com/v2/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CoinbaseApi.class);

        winkDexApi = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(WinkDexApi.class);
    }

    public ExchangeRate getBitcoinAverageRate() throws IOException {
        double rate = bitcoinAverageApi.getUsdTicker().execute().body().last;
        return new ExchangeRate("BitcoinAverage","USD", "BTC", rate);
    }

    public ExchangeRate getBitstampRate() throws IOException {
        double rate = bitstampApi.getTicker().execute().body().last;
        return new ExchangeRate("Bitstamp","USD", "BTC", rate);
    }

    public ExchangeRate getCoinbaseRate() throws IOException {
        double rate = coinbaseApi.getExchangeRates().execute().body().data.rates.get("USD");
        return new ExchangeRate("Coinbase","USD", "BTC", rate);
    }

    public ExchangeRate getWinkDexRate() throws IOException {
        double rate = (double) winkDexApi.getPrice().execute().body().getPrice() / 100.0f;
        return new ExchangeRate("WinkDex","USD", "BTC", rate);
    }
}