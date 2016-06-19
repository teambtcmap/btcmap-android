package com.bubelov.coins.dagger;

import android.content.Context;

import com.bubelov.coins.App;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Igor Bubelov
 * Date: 27/03/16 18:13
 */

@Module
public class AppContextModule {
    private App app;

    public AppContextModule(App app) {
        this.app = app;
    }

    @Provides @Singleton
    App app() {
        return app;
    }

    @Provides @Singleton
    Context context() {
        return app;
    }
}