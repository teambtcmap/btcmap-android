package com.bubelov.coins.dagger;

import com.bubelov.coins.serializer.DateTimeDeserializer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.joda.time.DateTime;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Igor Bubelov
 * Date: 23/03/16 01:07
 */

@Module
public class ConverterModule {
    @Provides @Singleton
    Gson provideGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .create();
    }
}