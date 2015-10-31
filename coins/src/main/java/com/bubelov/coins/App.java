package com.bubelov.coins;

import android.app.Application;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.database.DatabaseFactory;
import com.bubelov.coins.serializer.DateTimeDeserializer;
import com.bubelov.coins.service.sync.merchants.MerchantsSyncService;
import com.bubelov.coins.util.MainThreadBus;
import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;

import io.fabric.sdk.android.Fabric;
import org.joda.time.DateTime;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

/**
 * Author: Igor Bubelov
 * Date: 03/11/13
 */

public class App extends Application {
    private static App instance;

    private Bus bus;

    private CoinsApi api;

    private SQLiteOpenHelper databaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        instance = this;
        bus = new MainThreadBus();
        initApi();
        databaseHelper = DatabaseFactory.newHelper(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        startService(MerchantsSyncService.makeIntent(this, false));
    }

    public static App getInstance() {
        return instance;
    }

    public Bus getBus() {
        return bus;
    }

    public CoinsApi getApi() {
        return api;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return databaseHelper;
    }

    private void initApi() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .create();

        api = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CoinsApi.class);
    }
}