package sync

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import app.isDebuggable
import event.Event
import org.btcmap.R

class SyncNotificationController(private val context: Context) {

    fun showPostSyncNotifications(
        syncTimeMs: Long,
        newEvents: List<Event>,
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (context.isDebuggable()) {
            createSyncSummaryNotificationChannel(context)

            val intent = Intent(context, Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, SYNC_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.area_placeholder_icon)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Finished sync in $syncTimeMs ms. Got ${newEvents.size} new events.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context)
                .notify(SYNC_SUMMARY_NOTIFICATION_ID, builder.build())
        }

        createNewMerchantsNotificationChannel(context)

        newEvents.forEach { newEvent ->
            if (newEvent.type == "create") {
                val intent = Intent(context, Activity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                val builder = NotificationCompat.Builder(context, NEW_MERCHANTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.add_location)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText("There is a new place to spend sats in your area: ${newEvent.elementId}")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                NotificationManagerCompat.from(context)
                    .notify(newEvent.id.toInt(), builder.build())
            }
        }
    }

    fun showSyncFailedNotification(cause: Throwable) {
        if (context.isDebuggable()) {
            createSyncSummaryNotificationChannel(context)

            val intent = Intent(context, Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, SYNC_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.fmd_bad)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.sync_failed_s, cause.message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(SYNC_FAILED_NOTIFICATION_ID, builder.build())
                }
            }
        }
    }

    private fun createSyncSummaryNotificationChannel(context: Context) {
        val name = "Sync summary"
        val descriptionText = "Sync summary"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(SYNC_SUMMARY_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNewMerchantsNotificationChannel(context: Context) {
        val name = "New merchants"
        val descriptionText = "New merchants"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NEW_MERCHANTS_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val SYNC_SUMMARY_CHANNEL_ID = "sync_summary"
        private const val SYNC_SUMMARY_NOTIFICATION_ID = 1
        private const val SYNC_FAILED_NOTIFICATION_ID = 2
        private const val NEW_MERCHANTS_CHANNEL_ID = "new_merchants"
    }
}