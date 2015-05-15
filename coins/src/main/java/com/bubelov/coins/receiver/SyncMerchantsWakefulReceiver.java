package com.bubelov.coins.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.widget.Toast;

import com.bubelov.coins.service.DatabaseSyncService;

/**
 * Author: Igor Bubelov
 * Date: 15/07/14 21:56
 */

public class SyncMerchantsWakefulReceiver extends WakefulBroadcastReceiver {
    public static Intent makeIntent(Context context) {
        return new Intent(context, SyncMerchantsWakefulReceiver.class);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Starting merchants sync", Toast.LENGTH_LONG).show();
        startWakefulService(context, DatabaseSyncService.makeIntent(context));
    }
}
