package com.bubelov.coins.service;

import android.app.IntentService;
import android.database.sqlite.SQLiteOpenHelper;
import com.bubelov.coins.Api;
import com.bubelov.coins.App;

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

    protected SQLiteOpenHelper getDatabaseHelper() {
        return app.getDatabaseHelper();
    }

    protected Api getApi() {
        return app.getApi();
    }
}