package com.bubelov.coins.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.bubelov.coins.dagger.Injector

/**
 * @author Igor Bubelov
 */

class ClearPlaceNotificationsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Injector.mainComponent.placeNotificationsRepository().clear()
        context.unregisterReceiver(this)
    }

    companion object {
        val ACTION = "CLEAR_PLACE_NOTIFICATIONS"
    }
}