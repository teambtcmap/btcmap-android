package com.bubelov.coins.service.sync.merchants;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * Author: Igor Bubelov
 * Date: 17/02/16 16:58
 */

public class DatabaseGcmSyncService extends GcmTaskService {
    public static final String TAG = DatabaseGcmSyncService.class.getName();

    @Override
    public int onRunTask(TaskParams taskParams) {
        DatabaseSyncService.start(this);
        Answers.getInstance().logCustom(new CustomEvent("Started DB sync over GCM"));
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}