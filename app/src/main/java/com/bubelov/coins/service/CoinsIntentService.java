package com.bubelov.coins.service;

import android.app.IntentService;

import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.dagger.Injector;

/**
 * Author: Igor Bubelov
 * Date: 17/04/15 18:29
 */

public abstract class CoinsIntentService extends IntentService {
    public CoinsIntentService(String name) {
        super(name);
    }

    protected CoinsApi getApi() {
        return Injector.INSTANCE.getAppComponent().provideApi();
    }
}