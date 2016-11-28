package com.bubelov.coins.service.sync;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
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
        Answers.getInstance().logCustom(new CustomEvent("Initialized GCM sync task"));
        onRunTask(null);
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        DatabaseSyncService.start(this);
        Answers.getInstance().logCustom(new CustomEvent("Started DB sync over GCM"));
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}