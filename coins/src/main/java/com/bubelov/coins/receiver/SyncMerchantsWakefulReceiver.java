package com.bubelov.coins.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.bubelov.coins.service.MerchantsSyncService;

/**
 * Author: Igor Bubelov
 * Date: 15/07/14 21:56
 */

public class SyncMerchantsWakefulReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, MerchantsSyncService.makeIntent(context));
    }
}
