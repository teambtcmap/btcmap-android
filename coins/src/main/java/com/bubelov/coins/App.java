package com.bubelov.coins;

import android.app.Application;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.bubelov.coins.database.DatabaseHelper;
import com.bubelov.coins.manager.MerchantSyncManager;
import com.bubelov.coins.server.ServerFacade;
import com.bubelov.coins.server.osm.OsmServerFacade;

/**
 * Author: Igor Bubelov
 * Date: 03/11/13
 */

public class App extends Application {
    private ServerFacade serverFacade;

    private SQLiteOpenHelper databaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        serverFacade = new OsmServerFacade(this);
        databaseHelper = new DatabaseHelper(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        new MerchantSyncManager(this).scheduleAlarm();
    }

    public ServerFacade getServerFacade() {
        return serverFacade;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
