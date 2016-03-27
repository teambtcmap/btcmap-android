package com.bubelov.coins.dagger;

import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.provider.CoinsProvider;
import com.bubelov.coins.util.MapMarkersCache;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:57
 */

@Singleton
@Component(modules = {AppContextModule.class, DatabaseModule.class, CacheModule.class})
public interface AppComponent {
    SQLiteDatabase database();

    MerchantsCache getMerchantsCache();

    MapMarkersCache getMarkersCache();

    void inject(CoinsProvider provider);
    void inject(MerchantsCache cache);
}