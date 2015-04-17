package com.bubelov.coins;

import android.app.Application;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.database.DatabaseHelper;
import com.bubelov.coins.manager.MerchantSyncManager;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Author: Igor Bubelov
 * Date: 03/11/13
 */

public class App extends Application {
    private static App instance;

    private Api api;

    private SQLiteOpenHelper databaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        initApi();
        databaseHelper = new DatabaseHelper(this);
        new MerchantSyncManager(this).scheduleAlarm();
    }

    public static App getInstance() {
        return instance;
    }

    public Api getApi() {
        return api;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return databaseHelper;
    }

    private void initApi() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(getString(R.string.api_url))
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        api = restAdapter.create(Api.class);
    }
}