package com.bubelov.coins.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.R;
import com.bubelov.coins.data.api.coins.model.Place;
import com.bubelov.coins.data.api.coins.model.PlaceNotification;
import com.bubelov.coins.data.model.NotificationArea;
import com.bubelov.coins.receiver.ClearPlaceNotificationsReceiver;
import com.bubelov.coins.ui.activity.MapActivity;

import java.util.List;
import java.util.UUID;

/**
 * @author Igor Bubelov
 */

public class PlaceNotificationManager {
    private static final String NEW_PLACE_NOTIFICATION_GROUP = "NEW_PLACE";

    private final Context context;

    private final DataManager dataManager;

    public PlaceNotificationManager(Context context, DataManager dataManager) {
        this.context = context;
        this.dataManager = dataManager;
    }

    public boolean shouldNotifyUser(Place newPlace) {
        if (!newPlace.visible()) {
            return false;
        }

        NotificationArea notificationArea = dataManager.preferences().getNotificationArea();

        if (notificationArea == null) {
            return false;
        }

        float distance = DistanceUtils.getDistance(notificationArea.getCenter(), newPlace.getPosition());
        return distance <= notificationArea.getRadiusMeters();
    }

    public void notifyUser(long placeId, String placeName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_place))
                .setContentText(!TextUtils.isEmpty(placeName) ? placeName : context.getString(R.string.notification_new_place_no_name))
                .setDeleteIntent(prepareClearPlacesIntent())
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP);

        Intent intent = MapActivity.newShowPlaceIntent(context, placeId);
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
    }

    private void issueGroupNotification(List<PlaceNotification> pendingPlaces) {
        NotificationArea notificationArea = dataManager.preferences().getNotificationArea();

        Intent intent = MapActivity.newShowNotificationAreaIntent(context, notificationArea);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, NEW_PLACE_NOTIFICATION_GROUP.hashCode(), intent, 0);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        style.setBigContentTitle(context.getString(R.string.notification_new_places_content_title, String.valueOf(pendingPlaces.size())));

        for (PlaceNotification notification : pendingPlaces) {
            Place place = dataManager.database().getPlace(notification.getPlaceId());

            if (place != null) {
                // TODO add place name to place notification model
                style.addLine(place.name());
            }
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