package com.bubelov.coins.dagger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.provider.CoinsProvider;
import com.bubelov.coins.util.MapMarkersCache;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:57
 */

@Singleton
@Component(modules = {AppContextModule.class, DatabaseModule.class, CacheModule.class, ApiModule.class, ConverterModule.class})
public interface AppComponent {
    Context getContext();

    SQLiteDatabase database();

    MerchantsCache getMerchantsCache();

    MapMarkersCache getMarkersCache();

    CoinsApi provideApi();

    Gson provideGson();

    void inject(CoinsProvider provider);
    void inject(MerchantsCache cache);
}