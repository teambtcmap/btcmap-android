package com.bubelov.coins.sync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.dagger.Injector;

/**
 * @author Igor Bubelov
 */

public class DatabaseSyncService extends IntentService {
    public static void start(Context context) {
        context.startService(new Intent(context, DatabaseSyncService.class));
    }

    public DatabaseSyncService() {
        super(DatabaseSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        DatabaseSync databaseSync = Injector.INSTANCE.mainComponent().databaseSync();

        if (!databaseSync.isSyncing()) {
            databaseSync.sync();
        }
    }
}