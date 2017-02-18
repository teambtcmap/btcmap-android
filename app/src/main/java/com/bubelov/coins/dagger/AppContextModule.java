package com.bubelov.coins.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Author: Igor Bubelov
 * Date: 27/03/16 18:13
 */

@Module
public class AppContextModule {
    private Context context;

    public AppContextModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context context() {
        return context;
    }
}