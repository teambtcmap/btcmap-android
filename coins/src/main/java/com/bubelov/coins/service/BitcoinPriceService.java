package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.bubelov.coins.Constants;
import com.bubelov.coins.api.external.PriceResponse;
import com.bubelov.coins.api.external.WinkDexApi;
import com.bubelov.coins.database.Database;
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

    private WinkDexApi api;

    public BitcoinPriceService() {
        super(TAG);
        initApi();
    }

    public static Intent newInent(Context context, boolean forceLoad) {
        Intent intent = new Intent(context, BitcoinPriceService.class);
        intent.putExtra(FORCE_LOAD_EXTRA, forceLoad);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
        PriceResponse response = api.getPrice();

        ContentValues values = new ContentValues();
        values.put(Database.Currencies.PRICE, (double) response.getPrice() / 100.0);
        values.put(Database.Currencies.PRICE_LAST_CHECK, System.currentTimeMillis());

        getContentResolver().update(Database.Currencies.CONTENT_URI, values, selection, selectionArgs);
    }

    private void initApi() {
        Gson gson = new GsonBuilder()
                .setDateFormat(Constants.DATE_FORMAT)
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://winkdex.com/api/v0")
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(WinkDexApi.class);
    }
}