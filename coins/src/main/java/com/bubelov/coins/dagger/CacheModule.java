package com.bubelov.coins.dagger;

import com.bubelov.coins.MerchantsCache;
import com.bubelov.coins.util.MapMarkersCache;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:56
 */

@Module
public class CacheModule {
    @Provides @Singleton
    MerchantsCache merchantsCache() {
        return new MerchantsCache();
    }

    @Provides @Singleton
    MapMarkersCache markersCache() {
        return new MapMarkersCache();
    }
}