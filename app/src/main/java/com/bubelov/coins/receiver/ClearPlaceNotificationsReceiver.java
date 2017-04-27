package com.bubelov.coins.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.data.api.coins.model.PlaceNotification;

/**
 * @author Igor Bubelov
 */

public class ClearPlaceNotificationsReceiver extends BroadcastReceiver {
    public static final String ACTION = "PLACE_NOTIFICATION_DELETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        PlaceNotification.deleteAll();
        context.unregisterReceiver(this);
    }
}