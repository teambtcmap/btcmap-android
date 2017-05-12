package com.bubelov.coins.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;

import com.bubelov.coins.R;
import com.bubelov.coins.data.repository.area.NotificationAreaRepository;
import com.bubelov.coins.data.repository.notification.PlaceNotificationsRepository;
import com.bubelov.coins.domain.Place;
import com.bubelov.coins.domain.PlaceNotification;
import com.bubelov.coins.domain.NotificationArea;
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
    public PlaceNotificationManager(Context context, NotificationAreaRepository notificationAreaRepository, PlaceNotificationsRepository placeNotificationsRepository) {
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
                .setContentText(newPlace.name())
                .setDeleteIntent(prepareClearPlacesIntent())
                .setAutoCancel(true)
                .setGroup(NEW_PLACE_NOTIFICATION_GROUP);

        Intent intent = MapActivity.newShowPlaceIntent(context, newPlace.id());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build());

        PlaceNotification placeNotification = PlaceNotification.builder()
                .placeId(newPlace.id())
                .placeName(newPlace.name())
                .build();

        notificationsRepository.addNotification(placeNotification);

        Collection<PlaceNotification> notifications = notificationsRepository.getNotifications();

        if (notifications.size() > 1) {
            issueGroupNotification(notifications);
        }
    }

    private boolean shouldNotifyUser(Place newPlace) {
        if (!newPlace.visible()) {
            return false;
        }

        NotificationArea notificationArea = notificationAreaRepository.getNotificationArea();

        if (notificationArea == null) {
            return false;
        }

        float distance = DistanceUtils.getDistance(notificationArea.getCenter(), newPlace.getPosition());
        return distance <= notificationArea.getRadiusMeters();
    }

    private void issueGroupNotification(Collection<PlaceNotification> pendingPlaces) {
        NotificationArea notificationArea = notificationAreaRepository.getNotificationArea();

        Intent intent = MapActivity.newShowNotificationAreaIntent(context, notificationArea);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, NEW_PLACE_NOTIFICATION_GROUP.hashCode(), intent, 0);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        style.setBigContentTitle(context.getString(R.string.notification_new_places_content_title, String.valueOf(pendingPlaces.size())));

        for (PlaceNotification notification : pendingPlaces) {
            style.addLine(notification.placeName());
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