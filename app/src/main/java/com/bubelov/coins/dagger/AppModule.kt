/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.dagger

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager

import com.bubelov.coins.BuildConfig
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.db.Database
import com.bubelov.coins.util.JsonStringConverterFactory
import com.bubelov.coins.util.StringAdapter
import com.bubelov.coins.util.UtcDateTypeAdapter
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import java.util.Date

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
class AppModule {
    @Provides @Singleton
    fun providePlacesDb(database: Database) = database.placesDb()

    @Provides @Singleton
    fun provideFirebaseAnalytics(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides @Singleton
    fun provideGcmNetworkManager(context: Context): GcmNetworkManager {
        return GcmNetworkManager.getInstance(context)
    }

    @Provides @Singleton
    fun providePreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides @Singleton
    fun provideResources(context: Context): Resources = context.resources

    @Provides @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Date::class.java, UtcDateTypeAdapter())
            .registerTypeAdapter(String::class.java, StringAdapter())
            .create()
    }

    @Provides @Singleton
    fun provideCoinsApi(gson: Gson): CoinsApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(JsonStringConverterFactory(GsonConverterFactory.create()))
            .client(client)
            .build()
            .create(CoinsApi::class.java)
    }

//    @Provides @Singleton
//    fun provideCoinsApi(placesAssetsCache: PlacesAssetsCache): CoinsApi {
//        val retrofit = Retrofit.Builder()
//            .baseUrl(BuildConfig.API_URL)
//            .addConverterFactory(JsonStringConverterFactory(GsonConverterFactory.create()))
//            .build()
//
//        val behavior = NetworkBehavior.create()
//
//        val mockRetrofit = MockRetrofit.Builder(retrofit)
//            .networkBehavior(behavior)
//            .build()
//
//        val delegate = mockRetrofit.create(CoinsApi::class.java)
//
//        return MockCoinsApi(delegate, placesAssetsCache)
//    }
}