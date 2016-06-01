package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.R;
import com.bubelov.coins.api.CoinsApi;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Author: Igor Bubelov
 * Date: 01/06/16 22:07
 */

@Module
public class ApiModule {
    @Provides @Singleton
    CoinsApi provideApi(Gson gson, Context context) {
        return new Retrofit.Builder()
                .baseUrl(context.getString(R.string.api_url))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(CoinsApi.class);
    }
}
