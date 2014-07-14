package com.bubelov.coins.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.manager.MerchantSyncManager;

/**
 * Author: Igor Bubelov
 * Date: 10/07/14 21:22
 */

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MerchantSyncManager syncManager = new MerchantSyncManager(context);
        syncManager.scheduleAlarm();
    }
}
