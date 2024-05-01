package sync

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.isDebuggable
import conf.Conf
import conf.mapViewport
import element.ElementsRepo
import element.name
import kotlinx.coroutines.runBlocking
import org.btcmap.R
import java.time.Duration
import kotlin.random.Random

class SyncNotificationController(
    private val context: Context,
    private val elementsRepo: ElementsRepo,
) {

    fun showPostSyncNotifications(
        report: SyncReport,
        conf: Conf,
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (conf.showSyncSummary) {
            val syncTimeMillis = Duration.between(report.startedAt, report.finishedAt).toMillis()

            createSyncSummaryNotificationChannel(context)

            val builder = NotificationCompat.Builder(context, SYNC_SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.area_placeholder_icon)
                .setContentTitle("Finished sync")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            """
                                |Time: $syncTimeMillis ms
                                |Elements: ${report.elementsReport.newElements} new, ${report.elementsReport.updatedElements} updated in ${report.elementsReport.duration.toMillis()} ms
                                |Events: ${report.eventsReport.newEvents.size} new, ${report.eventsReport.updatedEvents} updated in ${report.eventsReport.duration.toMillis()} ms
                                |Reports: ${report.reportsReport.newReports} new, ${report.reportsReport.updatedReports} updated in ${report.reportsReport.duration.toMillis()} ms
                                |Areas: ${report.areasReport.newAreas} new, ${report.areasReport.updatedAreas} updated in ${report.areasReport.duration.toMillis()} ms
                                |Users: ${report.usersReport.newUsers} new, ${report.usersReport.updatedUsers} updated in ${report.usersReport.duration.toMillis()} ms
                            """.trimMargin()
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context)
                .notify(Random.Default.nextInt(1, Int.MAX_VALUE), builder.build())
        }

        if (conf.lastSyncDate == null) {
            return
        }

        createNewMerchantsNotificationChannel(context)

        report.eventsReport.newEvents.forEach { newEvent ->
            if (newEvent.type == "create") {
                val element =
                    runBlocking { elementsRepo.selectByOsmId(newEvent.elementId) } ?: return

                val distanceMeters = getDistanceInMeters(
                    startLatitude = conf.mapViewport().centerLatitude,
                    startLongitude = conf.mapViewport().centerLongitude,
                    endLatitude = element.lat,
                    endLongitude = element.lon,
                )

                val distanceThresholdMeters = if (conf.showAllNewElements) {
                    100_000_000f
                } else {
                    100_000f
                }

                if (distanceMeters > distanceThresholdMeters) {
                    return
                }

                val builder = NotificationCompat.Builder(context, NEW_MERCHANTS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.add_location)
                    .setContentTitle(element.name(context.resources))
                    .setContentText(context.getString(R.string.new_local_merchant_accepts_bitcoins))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                NotificationManagerCompat.from(context)
                    .notify(Random.Default.nextInt(1, Int.MAX_VALUE), builder.build())
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
                    notify(Random.Default.nextInt(1, Int.MAX_VALUE), builder.build())
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
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNewMerchantsNotificationChannel(context: Context) {
        val name = "New merchants"
        val descriptionText = "New merchants"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NEW_MERCHANTS_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getDistanceInMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val distance = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }

    companion object {
        private const val SYNC_SUMMARY_CHANNEL_ID = "sync_summary"
        private const val NEW_MERCHANTS_CHANNEL_ID = "new_merchants"
    }
}