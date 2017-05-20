package com.bubelov.coins.repository.rate;

import com.bubelov.coins.api.rates.BitcoinAverageApi;
import com.bubelov.coins.api.rates.BitstampApi;
import com.bubelov.coins.api.rates.CoinbaseApi;
import com.bubelov.coins.api.rates.WinkDexApi;
import com.bubelov.coins.model.ExchangeRate;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class ExchangeRatesRepository {
    private final BitcoinAverageApi bitcoinAverageApi;

    private final BitstampApi bitstampApi;

    private final CoinbaseApi coinbaseApi;

    private final WinkDexApi winkDexApi;

    @Inject
    ExchangeRatesRepository(OkHttpClient httpClient, Gson gson) {
        Converter.Factory converterFactory = GsonConverterFactory.create(gson);

        bitcoinAverageApi = new Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://api.bitcoinaverage.com/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitcoinAverageApi.class);

        bitstampApi = new Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitstampApi.class);

        coinbaseApi = new Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://api.coinbase.com/v2/")
                .addConverterFactory(converterFactory)
                .build()
                .create(CoinbaseApi.class);

        winkDexApi = new Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://winkdex.com/api/v0/")
                .addConverterFactory(converterFactory)
                .build()
                .create(WinkDexApi.class);
    }

    public Collection<ExchangeRate> getExchangeRates() {
        Collection<ExchangeRate> rates = new ArrayList<>();

        try {
            rates.add(getBitcoinAverageRate());
        } catch (IOException e) {
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getBitstampRate());
        } catch (IOException e) {
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getCoinbaseRate());
        } catch (IOException e) {
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getWinkDexRate());
        } catch (IOException e) {
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        return rates;
    }

    private ExchangeRate getBitcoinAverageRate() throws IOException {
        double rate = bitcoinAverageApi.getUsdTicker().execute().body().last;

        return ExchangeRate.builder()
                .id(0)
                .source("BitcoinAverage")
                .baseCurrencyCode("USD")
                .targetCurrencyCode("BTC")
                .rate(rate)
                .date(System.currentTimeMillis())
                .build();
    }

    private ExchangeRate getBitstampRate() throws IOException {
        double rate = bitstampApi.getTicker().execute().body().last;

        return ExchangeRate.builder()
                .id(0)
                .source("Bitstamp")
                .baseCurrencyCode("USD")
                .targetCurrencyCode("BTC")
                .rate(rate)
                .date(System.currentTimeMillis())
                .build();
    }

    private ExchangeRate getCoinbaseRate() throws IOException {
        double rate = coinbaseApi.getExchangeRates().execute().body().data.rates.get("USD");

        return ExchangeRate.builder()
                .id(0)
                .source("Coinbase")
                .baseCurrencyCode("USD")
                .targetCurrencyCode("BTC")
                .rate(rate)
                .date(System.currentTimeMillis())
                .build();
    }

    private ExchangeRate getWinkDexRate() throws IOException {
        double rate = (double) winkDexApi.getPrice().execute().body().getPrice() / 100.0f;

        return ExchangeRate.builder()
                .id(0)
                .source("WinkDex")
                .baseCurrencyCode("USD")
                .targetCurrencyCode("BTC")
                .rate(rate)
                .date(System.currentTimeMillis())
                .build();
    }
}