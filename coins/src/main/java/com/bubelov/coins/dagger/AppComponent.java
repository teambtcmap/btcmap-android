package com.bubelov.coins.dagger;

import com.bubelov.coins.util.MapMarkersCache;

import dagger.Component;

/**
 * Author: Igor Bubelov
 * Date: 26/03/16 17:57
 */

@Component(modules = CacheModule.class)
public interface AppComponent {
    MapMarkersCache getMarkersCache();
}
