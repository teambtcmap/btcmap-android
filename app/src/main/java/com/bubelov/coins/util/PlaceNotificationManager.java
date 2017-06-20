package com.bubelov.coins.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;

import com.bubelov.coins.R;
import com.bubelov.coins.repository.area.NotificationAreaRepository;
import com.bubelov.coins.repository.notification.PlaceNotificationsRepository;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceNotification;
import com.bubelov.coins.model.NotificationArea;
import com.bubelov.coins.ui.activity.MapActivity;

import java.util.Collection;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceNotificationManager {
    private static final String NEW_PLACE_NOTIFICATION_GROUP = "NEW_PLACE";

    private final Context context;

    private final NotificationAreaRepository notificationAreaRepository;

    private final PlaceNotificationsRepository notificationsRepository;

    @Inject
    PlaceNotificationManager(Context context, NotificationAreaRepository notificationAreaRepository, PlaceNotificationsRepository placeNotificationsRepository) {
        this.context = context;
        this.notificationAreaRepository = notificationAreaRepository;
        this.notificationsRepository = placeNotificationsRepository;
    }

    public void notifyUserIfNecessary(Place newPlace) {
        if (shouldNotifyUser(newPlace)) {
            notifyUser(newPlace);
        }
    }

    public void notifyUser(Place newPlace) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_place))
                .setContentText(newPlace.getName())
                .setDeleteIntent(prepareClearPlacesIntent())
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP);

        Intent intent = MapActivity.Companion.newIntent(context, newPlace.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build());

        PlaceNotification placeNotification = new PlaceNotification(newPlace.getId(), newPlace.getName());
        notificationsRepository.addNotification(placeNotification);

        Collection<PlaceNotification> notifications = notificationsRepository.getNotifications();

        if (notifications.size() > 1) {
            issueGroupNotification(notifications);
        }
    }

    private boolean shouldNotifyUser(Place newPlace) {
        if (!newPlace.getVisible()) {
            return false;
        }

        NotificationArea notificationArea = notificationAreaRepository.getNotificationArea();

        if (notificationArea == null) {
            return false;
        }

        return DistanceUtils.INSTANCE.getDistance(
                notificationArea.getLatitude(),
                notificationArea.getLongitude(),
                newPlace.getLatitude(),
                newPlace.getLongitude()
        ) <= notificationArea.getRadius();
    }

    private void issueGroupNotification(Collection<PlaceNotification> pendingPlaces) {
        Intent intent = MapActivity.Companion.newIntent(context, 0);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, NEW_PLACE_NOTIFICATION_GROUP.hashCode(), intent, 0);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        style.setBigContentTitle(context.getString(R.string.notification_new_places_content_title, String.valueOf(pendingPlaces.size())));

        for (PlaceNotification notification : pendingPlaces) {
            style.addLine(notification.getPlaceName());
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