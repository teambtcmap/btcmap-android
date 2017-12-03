package com.bubelov.coins.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat

import com.bubelov.coins.R
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.model.Place
import com.bubelov.coins.ui.activity.MapActivity

import javax.inject.Inject
import javax.inject.Singleton
import android.app.NotificationChannel
import android.os.Build

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceNotificationManager @Inject
internal constructor(
        private val context: Context,
        private val notificationAreaRepository: NotificationAreaRepository
) {
    init {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(NEW_PLACE_NOTIFICATIONS_CHANNEL,
                    context.getString(R.string.new_place_notifications),
                    NotificationManager.IMPORTANCE_LOW).apply {
                lightColor = context.getColor(R.color.primary)
                enableVibration(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifyUserIfNecessary(newPlace: Place) {
        if (shouldNotifyUser(newPlace)) {
            notifyUser(newPlace)
        }
    }

    fun notifyUser(place: Place) {
        val builder = NotificationCompat.Builder(context, NEW_PLACE_NOTIFICATIONS_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_place))
                .setContentText(place.name)
                .setAutoCancel(true)

        val intent = MapActivity.newIntent(context, place.id)
        val pendingIntent = PendingIntent.getActivity(context, place.id.toInt(), intent, 0)
        builder.setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(place.id.toInt(), builder.build())
    }

    private fun shouldNotifyUser(newPlace: Place): Boolean {
        if (!newPlace.visible) {
            return false
        }

        val notificationArea = notificationAreaRepository.notificationArea ?: return false

        return DistanceUtils.getDistance(
                notificationArea.latitude,
                notificationArea.longitude,
                newPlace.latitude,
                newPlace.longitude
        ) <= notificationArea.radius
    }

    companion object {
        private val NEW_PLACE_NOTIFICATIONS_CHANNEL = "new_place_notifications"
    }
}