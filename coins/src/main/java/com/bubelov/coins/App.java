package com.bubelov.coins;

import android.app.Application;
import android.preference.PreferenceManager;

import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.serializer.DateTimeDeserializer;
import com.bubelov.coins.util.MainThreadBus;
import com.crashlytics.android.Crashlytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;

import org.joda.time.DateTime;

import io.fabric.sdk.android.Fabric;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * Author: Igor Bubelov
 * Date: 03/11/13
 */

public class App extends Application {
    private static App instance;

    private Bus bus;

    private CoinsApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Fabric.with(this, new Crashlytics());

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Injector.INSTANCE.initAppComponent(this);

        bus = new MainThreadBus();
        initApi();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
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

    private void initApi() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .create();

        api = new Retrofit.Builder()
                .baseUrl(getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CoinsApi.class);
    }
}