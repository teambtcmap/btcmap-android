package com.bubelov.coins.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.service.sync.merchants.MerchantsSyncService;

/**
 * Author: Igor Bubelov
 * Date: 10/07/14 21:22
 */

public class SyncDatabaseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(MerchantsSyncService.makeIntent(context, false));
    }
}
