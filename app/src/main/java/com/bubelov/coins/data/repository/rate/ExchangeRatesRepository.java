package com.bubelov.coins.data.repository.rate;

import com.bubelov.coins.data.api.rates.BitcoinAverageApi;
import com.bubelov.coins.data.api.rates.BitstampApi;
import com.bubelov.coins.data.api.rates.CoinbaseApi;
import com.bubelov.coins.data.api.rates.WinkDexApi;
import com.bubelov.coins.domain.ExchangeRate;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    ExchangeRatesRepository(Gson gson) {
        Converter.Factory converterFactory = GsonConverterFactory.create(gson);

        bitcoinAverageApi = new Retrofit.Builder()
                .baseUrl("https://api.bitcoinaverage.com/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitcoinAverageApi.class);

        bitstampApi = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addConverterFactory(converterFactory)
                .build()
                .create(BitstampApi.class);

        coinbaseApi = new Retrofit.Builder()
                .baseUrl("https://api.coinbase.com/v2/")
                .addConverterFactory(converterFactory)
                .build()
                .create(CoinbaseApi.class);

        winkDexApi = new Retrofit.Builder()
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
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getBitstampRate());
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getCoinbaseRate());
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        try {
            rates.add(getWinkDexRate());
        } catch (IOException e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't fetch exchange rate");
        }

        return rates;
    }

    private ExchangeRate getBitcoinAverageRate() throws IOException {
        double rate = bitcoinAverageApi.getUsdTicker().execute().body().last;
        return new ExchangeRate("BitcoinAverage","USD", "BTC", rate);
    }

    private ExchangeRate getBitstampRate() throws IOException {
        double rate = bitstampApi.getTicker().execute().body().last;
        return new ExchangeRate("Bitstamp","USD", "BTC", rate);
    }

    private ExchangeRate getCoinbaseRate() throws IOException {
        double rate = coinbaseApi.getExchangeRates().execute().body().data.rates.get("USD");
        return new ExchangeRate("Coinbase","USD", "BTC", rate);
    }

    private ExchangeRate getWinkDexRate() throws IOException {
        double rate = (double) winkDexApi.getPrice().execute().body().getPrice() / 100.0f;
        return new ExchangeRate("WinkDex","USD", "BTC", rate);
    }
}