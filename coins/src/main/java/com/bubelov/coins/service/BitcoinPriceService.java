package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.bubelov.coins.api.external.BitcoinAverageApi;
import com.bubelov.coins.api.external.BitcoinAverageTickerResponse;
import com.bubelov.coins.api.external.WinkDexPriceResponse;
import com.bubelov.coins.api.external.WinkDexApi;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.util.Utils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:44
 */

public class BitcoinPriceService extends CoinsIntentService {
    private static final String TAG = BitcoinPriceService.class.getSimpleName();

    private static final String FORCE_LOAD_EXTRA = "force_load";

    private static final long CACHE_LIFETIME_IN_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private static final String selection = "code = ?";
    private static final String[] selectionArgs = new String[] { "BTC" };

    private WinkDexApi winkDexApi;

    private BitcoinAverageApi bitcoinAverageApi;

    public BitcoinPriceService() {
        super(TAG);
        initApis();
    }

    public static Intent newIntent(Context context, boolean forceLoad) {
        Intent intent = new Intent(context, BitcoinPriceService.class);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!Utils.isOnline(this)) {
            return;
        }

        if (intent.getBooleanExtra(FORCE_LOAD_EXTRA, false)) {
            loadPrice();
        } else {
            Cursor lastCheckCursor = getContentResolver().query(Database.Currencies.CONTENT_URI,
                    new String[]{Database.Currencies.PRICE_LAST_CHECK},
                    selection,
                    selectionArgs,
                    null);

            if (lastCheckCursor.moveToNext()) {
                if (lastCheckCursor.isNull(0) || System.currentTimeMillis() - lastCheckCursor.getLong(0) > CACHE_LIFETIME_IN_MILLIS) {
                    loadPrice();
                }
            }

            lastCheckCursor.close();
        }
    }

    private void loadPrice() {
        float price;

        WinkDexPriceResponse winkDexResponse = winkDexApi.getPrice();

        // Sometimes this shitty API returns 0!
        if (winkDexResponse.getPrice() != 0) {
            price = (float) winkDexResponse.getPrice() / 100.0f;
        } else {
            BitcoinAverageTickerResponse bitcoinAverageResponse = bitcoinAverageApi.getUsdTicker();
            price = bitcoinAverageResponse.getLast();
        }

        if (price == 0.0f) {
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
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://winkdex.com/api/v0")
                .setConverter(new GsonConverter(getGsonForApis()))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        winkDexApi = restAdapter.create(WinkDexApi.class);
    }

    private void initBitcoinAverageApi() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.bitcoinaverage.com")
                .setConverter(new GsonConverter(getGsonForApis()))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        bitcoinAverageApi = restAdapter.create(BitcoinAverageApi.class);
    }

    private Gson getGsonForApis() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }
}