package com.bubelov.coins.dagger;

import com.bubelov.coins.PlacesCache;
import com.bubelov.coins.util.MapMarkersCache;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author Igor Bubelov
 */

@Module
public class CacheModule {
    @Provides @Singleton
    PlacesCache placesCache() {
        return new PlacesCache();
    }

    @Provides @Singleton
    MapMarkersCache markersCache() {
        return new MapMarkersCache();
    }
}