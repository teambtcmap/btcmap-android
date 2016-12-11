package com.bubelov.coins.service.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceNotification;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.provider.NotificationAreaProvider;
import com.bubelov.coins.receiver.ClearPlaceNotificationsReceiver;
import com.bubelov.coins.ui.activity.MapActivity;
import com.bubelov.coins.util.DistanceUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class UserNotificationController {
    public static final String NEW_PLACE_NOTIFICATION_GROUP = "NEW_PLACE";

    private Context context;

    public UserNotificationController(Context context) {
        this.context = context;
    }

    public boolean shouldNotifyUser(Place place) {
        NotificationArea notificationArea = new NotificationAreaProvider(context).get();

        if (notificationArea == null) {
            Timber.d("Notification area was not set");
            return false;
        }

        Timber.d("Notification area center: %s", notificationArea.getCenter());
        Timber.d("Notification area radius: %s", notificationArea.getRadiusMeters());

        float distance = DistanceUtils.getDistance(notificationArea.getCenter(), place.getPosition());

        Timber.d("Distance: %s", distance);

        if (distance > notificationArea.getRadiusMeters()) {
            return false;
        }

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        return DatabaseUtils.queryNumEntries(db, DbContract.Places.TABLE_NAME, "_id = ?", new String[]{String.valueOf(place.getId())}) == 0;
    }

    public void notifyUser(long placeId, String placeName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_place))
                .setContentText(!TextUtils.isEmpty(placeName) ? placeName : context.getString(R.string.notification_new_place_no_name))
                .setDeleteIntent(prepareClearPlacesIntent())
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP);

        Intent intent = MapActivity.newShowPlaceIntent(context, placeId, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build());

        PlaceNotification placeNotification = new PlaceNotification();
        placeNotification.setPlaceId(placeId);
        PlaceNotification.insert(placeNotification);

        if (PlaceNotification.queryForAll().size() > 1) {
            issueGroupNotification(PlaceNotification.queryForAll());
        }

        Answers.getInstance().logCustom(new CustomEvent("Notified user about new place")
                .putCustomAttribute("id", placeId));
    }

    private void issueGroupNotification(List<PlaceNotification> pendingPlaces) {
        NotificationArea notificationArea = new NotificationAreaProvider(context).get();

        Intent intent = MapActivity.newShowNotificationAreaIntent(context, notificationArea, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, NEW_PLACE_NOTIFICATION_GROUP.hashCode(), intent, 0);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        style.setBigContentTitle(context.getString(R.string.notification_new_places_content_title, String.valueOf(pendingPlaces.size())));

        for (PlaceNotification notification : pendingPlaces) {
            Place place = Place.find(notification.getPlaceId());
            style.addLine(place.getName());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setContentTitle(context.getString(R.string.notification_new_places_content_title, String.valueOf(pendingPlaces.size())))
                .setContentText(context.getString(R.string.notification_new_places_content_text))
                .setDeleteIntent(prepareClearPlacesIntent())
                .setStyle(style)
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP)
                .setGroupSummary(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NEW_PLACE_NOTIFICATION_GROUP.hashCode(), builder.build());
    }

    private PendingIntent prepareClearPlacesIntent() {
        Intent deleteIntent = new Intent(ClearPlaceNotificationsReceiver.ACTION);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);
        context.registerReceiver(new ClearPlaceNotificationsReceiver(), new IntentFilter(ClearPlaceNotificationsReceiver.ACTION));
        return deletePendingIntent;
    }
}