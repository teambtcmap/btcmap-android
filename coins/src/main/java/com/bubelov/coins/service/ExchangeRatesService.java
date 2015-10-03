package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bubelov.coins.R;
import com.bubelov.coins.api.external.BitcoinAverageApi;
import com.bubelov.coins.api.external.BitstampApi;
import com.bubelov.coins.api.external.CoinbaseApi;
import com.bubelov.coins.api.external.WinkDexApi;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.util.Utils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:44
 */

public class ExchangeRatesService extends CoinsIntentService {
    private static final String TAG = ExchangeRatesService.class.getSimpleName();

    private static final String SOURCE_CURRENCY_CODE_EXTRA = "source_currency_code";

    private static final String TARGET_CURRENCY_CODE_EXTRA = "target_currency_code";

    private static final String FORCE_LOAD_EXTRA = "force_load";

    private static final long CACHE_LIFETIME_IN_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private BitstampApi bitstampApi;
    private CoinbaseApi coinbaseApi;
    private BitcoinAverageApi bitcoinAverageApi;
    private WinkDexApi winkDexApi;

    public ExchangeRatesService() {
        super(TAG);
        initApis();
    }

    public static Intent newIntent(Context context, String sourceCurrencyCode, String targetCurrencyCode, boolean forceLoad) {
        Intent intent = new Intent(context, ExchangeRatesService.class);
        intent.putExtra(SOURCE_CURRENCY_CODE_EXTRA, sourceCurrencyCode);
        intent.putExtra(TARGET_CURRENCY_CODE_EXTRA, targetCurrencyCode);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Utils.isOnline(this)) {
            return;
        }

        String sourceCurrencyCode = intent.getStringExtra(SOURCE_CURRENCY_CODE_EXTRA);
        String targetCurrencyCode = intent.getStringExtra(TARGET_CURRENCY_CODE_EXTRA);

        long sourceCurrencyId = getCurrencyId(sourceCurrencyCode);
        long targetCurrencyId = getCurrencyId(targetCurrencyCode);

        if (sourceCurrencyId == -1 || targetCurrencyId == -1) {
            Log.i(TAG, "Couldn't find currency pair");
            return;
        }

        if (intent.getBooleanExtra(FORCE_LOAD_EXTRA, false) || !isCacheUpToDate(sourceCurrencyId, targetCurrencyId)) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            ExchangeRatesProviderType provider = ExchangeRatesProviderType.valueOf(preferences.getString(getString(R.string.pref_exchange_rates_provider_key), null));

            try {
                float exchangeRate = loadExchangeRate(sourceCurrencyCode, targetCurrencyCode, provider);
                long now = System.currentTimeMillis();

                ContentValues values = new ContentValues();
                values.put(Database.ExchangeRates.SOURCE_CURRENCY_ID, sourceCurrencyId);
                values.put(Database.ExchangeRates.TARGET_CURRENCY_ID, targetCurrencyId);
                values.put(Database.ExchangeRates.VALUE, exchangeRate);
                values.put(Database.ExchangeRates._CREATED_AT, now);
                values.put(Database.ExchangeRates._UPDATED_AT, now);

                getContentResolver().insert(Database.ExchangeRates.CONTENT_URI, values);
            } catch (IOException exception) {
                Log.e(TAG, "Couldn't load exchange rate", exception);
            }
        }
    }

    private boolean isCacheUpToDate(long sourceCurrencyId, long targetCurrencyId) {
        Cursor ratesCursor = getContentResolver().query(Database.ExchangeRates.CONTENT_URI,
                new String[]{Database.ExchangeRates._UPDATED_AT, Database.ExchangeRates.VALUE},
                String.format("%s = ? and %s = ?", Database.ExchangeRates.SOURCE_CURRENCY_ID, Database.ExchangeRates.TARGET_CURRENCY_ID),
                new String[]{String.valueOf(sourceCurrencyId), String.valueOf(targetCurrencyId)},
                String.format("%s DESC", Database.ExchangeRates._UPDATED_AT));

        if (!ratesCursor.moveToNext()) {
            ratesCursor.close();
            return false;
        } else {
            long lastCheckTime = ratesCursor.getLong(ratesCursor.getColumnIndex(Database.ExchangeRates._UPDATED_AT));

            if (System.currentTimeMillis() - lastCheckTime > CACHE_LIFETIME_IN_MILLIS) {
                ratesCursor.close();
                return false;
            } else {
                return true;
            }
        }
    }

    private float loadExchangeRate(String sourceCurrencyCode, String targetCurrencyCode, ExchangeRatesProviderType provider) throws IOException {
        float exchangeRate = 0;

        if (provider.equals(ExchangeRatesProviderType.COINBASE)) {
            exchangeRate = coinbaseApi.getTicker().execute().body().getPrice();
        } else if (provider.equals(ExchangeRatesProviderType.BITSTAMP)) {
            exchangeRate = bitstampApi.getTicker().execute().body().getLast();
        } else if (provider.equals(ExchangeRatesProviderType.BITCOIN_AVERAGE)) {
            exchangeRate = bitcoinAverageApi.getUsdTicker().execute().body().getLast();
        } else if (provider.equals(ExchangeRatesProviderType.WINKDEX)) {
            exchangeRate = (float) winkDexApi.getPrice().execute().body().getPrice() / 100.0f;
        }

        return exchangeRate;
    }

    private long getCurrencyId(String code) {
        Cursor cursor = getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID},
                String.format("%s = ?", Database.Currencies.CODE),
                new String[]{code},
                null);

        try {
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    private void initApis() {
        initBitstampApi();
        initCoinbaseApi();
        initBitcoinAverageApi();
        initWinkDexApi();
    }

    private void initBitstampApi() {
        bitstampApi = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/api/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(BitstampApi.class);
    }

    private void initCoinbaseApi() {
        coinbaseApi = new Retrofit.Builder()
                .baseUrl("https://api.exchange.coinbase.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(CoinbaseApi.class);
    }

    private void initBitcoinAverageApi() {
        bitcoinAverageApi = new Retrofit.Builder()
                .baseUrl("https://api.bitcoinaverage.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(BitcoinAverageApi.class);
    }

    private void initWinkDexApi() {
        winkDexApi = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(WinkDexApi.class);
    }

    private Gson getGsonForApis() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}