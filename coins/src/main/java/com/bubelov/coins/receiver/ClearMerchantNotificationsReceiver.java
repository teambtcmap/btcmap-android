package com.bubelov.coins.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.dao.MerchantNotificationDAO;

/**
 * Author: Igor Bubelov
 * Date: 10/30/15 1:59 PM
 */

public class ClearMerchantNotificationsReceiver extends BroadcastReceiver {
    public static final String ACTION = "MERCHANT_NOTIFICATION_DELETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        new MerchantNotificationDAO(context).deleteAll();
        context.unregisterReceiver(this);
    }
}