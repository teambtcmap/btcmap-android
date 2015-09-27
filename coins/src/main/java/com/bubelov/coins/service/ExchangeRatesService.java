package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.bubelov.coins.api.external.BitcoinAverageApi;
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

    private static final String PROVIDER_EXTRA = "provider";

    private static final String FORCE_LOAD_EXTRA = "force_load";

    private static final long CACHE_LIFETIME_IN_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private static final String selection = String.format("%s = ?", Database.Currencies.CODE);
    private static final String[] selectionArgs = new String[] { "BTC" };

    private WinkDexApi winkDexApi;

    private BitcoinAverageApi bitcoinAverageApi;

    public ExchangeRatesService() {
        super(TAG);
        initApis();
    }

    public static Intent newIntent(Context context, ExchangeRatesSource provider, boolean forceLoad) {
        Intent intent = new Intent(context, ExchangeRatesService.class);
        intent.putExtra(PROVIDER_EXTRA, provider);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Utils.isOnline(this)) {
            return;
        }

        ExchangeRatesSource exchangeRatesSource = ((ExchangeRatesSource) intent.getSerializableExtra(PROVIDER_EXTRA));

        if (intent.getBooleanExtra(FORCE_LOAD_EXTRA, false)) {
            try {
                loadPrice(exchangeRatesSource);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Cursor lastCheckCursor = getContentResolver().query(Database.Currencies.CONTENT_URI,
                    new String[]{Database.Currencies.PRICE_LAST_CHECK},
                    selection,
                    selectionArgs,
                    null);

            if (lastCheckCursor.moveToNext()) {
                if (lastCheckCursor.isNull(0) || System.currentTimeMillis() - lastCheckCursor.getLong(0) > CACHE_LIFETIME_IN_MILLIS) {
                    try {
                        loadPrice(exchangeRatesSource);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            lastCheckCursor.close();
        }
    }

    private void loadPrice(ExchangeRatesSource provider) throws IOException {
        float price = 0;

        if (provider.equals(ExchangeRatesSource.WINKDEX)) {
            price = (float) winkDexApi.getPrice().execute().body().getPrice() / 100.0f;
        } else if (provider.equals(ExchangeRatesSource.BITCOIN_AVERAGE)) {
            price = bitcoinAverageApi.getUsdTicker().execute().body().getLast();
        }

        if (price == 0) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(Database.Currencies.PRICE, price);
        values.put(Database.Currencies.PRICE_LAST_CHECK, System.currentTimeMillis());

        getContentResolver().update(Database.Currencies.CONTENT_URI, values, selection, selectionArgs);
    }

    private void initApis() {
        initWinkDexApi();
        initBitcoinAverageApi();
    }

    private void initWinkDexApi() {
        winkDexApi = new Retrofit.Builder()
                .baseUrl("https://winkdex.com/api/v0/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(WinkDexApi.class);
    }

    private void initBitcoinAverageApi() {
        bitcoinAverageApi = new Retrofit.Builder()
                .baseUrl("https://api.bitcoinaverage.com/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())

                .addConverterFactory(GsonConverterFactory.create(getGsonForApis()))
                .build()
                .create(BitcoinAverageApi.class);
    }

    private Gson getGsonForApis() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}