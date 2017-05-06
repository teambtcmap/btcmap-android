package com.bubelov.coins.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.bubelov.coins.dagger.Injector;

/**
 * @author Igor Bubelov
 */

public class ClearPlaceNotificationsReceiver extends BroadcastReceiver {
    public static final String ACTION = "PLACE_NOTIFICATION_DELETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Injector.INSTANCE.mainComponent().placeNotificationsRepository().clear();
        context.unregisterReceiver(this);
    }
}