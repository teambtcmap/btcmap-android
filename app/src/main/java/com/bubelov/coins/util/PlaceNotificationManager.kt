package com.bubelov.coins.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.NotificationCompat

import com.bubelov.coins.R
import com.bubelov.coins.repository.area.NotificationAreaRepository
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository
import com.bubelov.coins.model.Place
import com.bubelov.coins.model.PlaceNotification
import com.bubelov.coins.ui.activity.MapActivity
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceNotificationManager @Inject
internal constructor(private val context: Context, private val notificationAreaRepository: NotificationAreaRepository, private val notificationsRepository: PlaceNotificationsRepository) {
    fun notifyUserIfNecessary(newPlace: Place) {
        if (shouldNotifyUser(newPlace)) {
            notifyUser(newPlace)
        }
    }

    fun notifyUser(newPlace: Place) {
        val builder = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_place))
                .setContentText(newPlace.name)
                .setDeleteIntent(prepareClearPlacesIntent())
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP)

        val intent = MapActivity.newIntent(context, newPlace.id)
        val pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0)
        builder.setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build())

        val placeNotification = PlaceNotification(newPlace.id, newPlace.name)
        notificationsRepository.addNotification(placeNotification)

        val notifications = notificationsRepository.notifications

        if (notifications.size > 1) {
            issueGroupNotification(notifications)
        }
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

    private fun issueGroupNotification(pendingPlaces: Collection<PlaceNotification>) {
        val intent = MapActivity.newIntent(context, 0)
        val pendingIntent = PendingIntent.getActivity(context, NEW_PLACE_NOTIFICATION_GROUP.hashCode(), intent, 0)

        val style = NotificationCompat.InboxStyle()
        style.setBigContentTitle(context.getString(R.string.notification_new_places_content_title, pendingPlaces.size.toString()))

        for ((_, placeName) in pendingPlaces) {
            style.addLine(placeName)
        }

        val builder = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getString(R.string.notification_new_places_content_title, pendingPlaces.size.toString()))
                .setContentText(context.getString(R.string.notification_new_places_content_text))
                .setDeleteIntent(prepareClearPlacesIntent())
                .setStyle(style)
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP)
                .setGroupSummary(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NEW_PLACE_NOTIFICATION_GROUP.hashCode(), builder.build())
    }

    private fun prepareClearPlacesIntent(): PendingIntent {
        val deleteIntent = Intent(ClearPlaceNotificationsBroadcastReceiver.ACTION)
        val deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0)
        context.registerReceiver(ClearPlaceNotificationsBroadcastReceiver(), IntentFilter(ClearPlaceNotificationsBroadcastReceiver.ACTION))
        return deletePendingIntent
    }

    companion object {
        private val NEW_PLACE_NOTIFICATION_GROUP = "NEW_PLACE"
    }
}