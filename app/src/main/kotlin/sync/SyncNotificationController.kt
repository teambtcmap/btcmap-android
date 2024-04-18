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
import element.ElementsRepo
import org.btcmap.R

class SyncNotificationController(private val context: Context) {

    fun showPostSyncNotifications(elementsSyncReport: ElementsRepo.SyncReport?) {
        if (elementsSyncReport == null) {
            return
        }

        if (context.isDebuggable()) {
            createSyncSummaryNotificationChannel(context)
        }

        createNewMerchantsNotificationChannel(context)

        val intent = Intent(context, Activity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, SYNC_SUMMARY_CHANNEL_ID)
            .setSmallIcon(R.drawable.area_placeholder_icon)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(elementsSyncReport.toString())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(SYNC_SUMMARY_NOTIFICATION_ID, builder.build())
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
        private const val NEW_MERCHANTS_CHANNEL_ID = "new_merchants"
    }
}