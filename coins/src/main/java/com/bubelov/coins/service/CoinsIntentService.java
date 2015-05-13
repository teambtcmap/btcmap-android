package com.bubelov.coins.service;

import android.app.IntentService;
import android.database.sqlite.SQLiteOpenHelper;
import com.bubelov.coins.api.CoinsApi;
import com.bubelov.coins.App;
import com.squareup.otto.Bus;

/**
 * Author: Igor Bubelov
 * Date: 17/04/15 18:29
 */

public abstract class CoinsIntentService extends IntentService {
    private App app;

    public CoinsIntentService(String name) {
        super(name);
        app = App.getInstance();
    }

    public App getApp() {
        return app;
    }

    protected Bus getBus() {
        return app.getBus();
    }

    protected CoinsApi getApi() {
        return app.getApi();
    }

    protected SQLiteOpenHelper getDatabaseHelper() {
        return app.getDatabaseHelper();
    }
}