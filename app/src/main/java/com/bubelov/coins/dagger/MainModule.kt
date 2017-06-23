package com.bubelov.coins.dagger

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.preference.PreferenceManager

import com.bubelov.coins.BuildConfig
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.database.DbHelper
import com.bubelov.coins.util.StringAdapter
import com.bubelov.coins.util.UtcDateTypeAdapter
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import java.util.Date
import java.util.concurrent.TimeUnit

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author Igor Bubelov
 */

@Module
class MainModule(private val context: Context) {
    @Provides
    @Singleton
    internal fun context(): Context {
        return context
    }

    @Provides
    @Singleton
    internal fun analytics(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    internal fun sqlDatabase(context: Context): SQLiteDatabase {
        return DbHelper(context).writableDatabase
    }

    @Provides
    @Singleton
    internal fun preferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    internal fun gson(): Gson {
        return GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date::class.java, UtcDateTypeAdapter())
                .registerTypeAdapter(String::class.java, StringAdapter())
                .create()
    }

    @Provides
    @Singleton
    internal fun coinsApi(httpClient: OkHttpClient, gson: Gson): CoinsApi {
        return Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build()
                .create(CoinsApi::class.java)
    }

    @Provides
    @Singleton
    internal fun httpClient(): OkHttpClient {
        val httpClientBuilder = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
            httpClientBuilder.addInterceptor(loggingInterceptor)
            httpClientBuilder.addNetworkInterceptor(StethoInterceptor())
        }

        return httpClientBuilder.build()
    }
}