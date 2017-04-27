package com.bubelov.coins.data;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.data.api.coins.CoinsApi;
import com.bubelov.coins.data.db.Database;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 */

public class DataManager {
    private final CoinsApi coinsApi;

    private final RatesApi ratesApi;

    private final Database database;

    private final Preferences preferences;

    public DataManager(SQLiteDatabase db, SharedPreferences preferences, Gson gson) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            httpClientBuilder.addInterceptor(loggingInterceptor);
        }

        coinsApi = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClientBuilder.build())
                .build()
                .create(CoinsApi.class);

        database = new Database(db);
        this.preferences = new Preferences(preferences, gson);
        ratesApi = new RatesApi(gson);
    }

    public CoinsApi coinsApi() {
        return coinsApi;
    }

    public RatesApi ratesApi() {
        return ratesApi;
    }

    public Database database() {
        return database;
    }

    public Preferences preferences() {
        return preferences;
    }
}