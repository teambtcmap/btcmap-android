package com.bubelov.coins.sync;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * @author Igor Bubelov
 */

public class DatabaseGcmSyncService extends GcmTaskService {
    public static final String TAG = DatabaseGcmSyncService.class.getName();

    @Override
    public void onInitializeTasks() {
        onRunTask(null);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        DatabaseSyncService.start(this);
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}