package com.bubelov.coins.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.BuildConfig;
import com.bubelov.coins.data.api.coins.CoinsApi;
import com.bubelov.coins.data.database.AssetDbHelper;
import com.bubelov.coins.data.database.DbHelper;
import com.bubelov.coins.util.AutoValueAdapterFactory;
import com.bubelov.coins.service.DatabaseSync;
import com.bubelov.coins.util.StringAdapter;
import com.bubelov.coins.util.UtcDateTypeAdapter;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Igor Bubelov
 */

@Module
public class MainModule {
    private Context context;

    public MainModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context context() {
        return context;
    }

    @Provides
    @Singleton
    DatabaseSync databaseSync() {
        return new DatabaseSync();
    }

    @Provides
    @Singleton
    FirebaseAnalytics analytics(Context context) {
        return FirebaseAnalytics.getInstance(context);
    }

    @Provides
    @Singleton
    SQLiteDatabase sqlDatabase(Context context) {
        SQLiteOpenHelper helper = BuildConfig.USE_DB_SNAPSHOT ? new AssetDbHelper(context) : new DbHelper(context);
        return helper.getWritableDatabase();
    }

    @Provides
    @Singleton
    SharedPreferences preferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    Gson gson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new AutoValueAdapterFactory())
                .registerTypeAdapter(Date.class, new UtcDateTypeAdapter())
                .registerTypeAdapter(String.class, new StringAdapter())
                .create();
    }

    @Provides
    @Singleton
    CoinsApi coinsApi(OkHttpClient httpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()
                .create(CoinsApi.class);
    }

    @Provides
    @Singleton
    OkHttpClient httpClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            httpClientBuilder.addInterceptor(loggingInterceptor);
            httpClientBuilder.addNetworkInterceptor(new StethoInterceptor());
        }

        return httpClientBuilder.build();
    }
}