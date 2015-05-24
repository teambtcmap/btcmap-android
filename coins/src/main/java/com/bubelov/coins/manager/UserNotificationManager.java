package com.bubelov.coins.manager;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.activity.MapActivity;
import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 14:46
 */

public class UserNotificationManager {
    private static final String TAG = UserNotificationManager.class.getName();

    private static final int DEFAULT_RADIUS_METERS = 50000;

    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";

    private static final String RADIUS_KEY = "radius";

    private Context context;
    private SharedPreferences preferences;

    public UserNotificationManager(Context context) {
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public LatLng getNotificationAreaCenter() {
        if (!preferences.contains(LATITUDE_KEY) || !preferences.contains(LONGITUDE_KEY)) {
            return null;
        }

        return new LatLng(preferences.getFloat(LATITUDE_KEY, 0), preferences.getFloat(LONGITUDE_KEY, 0));
    }

    public void setNotificationAreaCenter(LatLng center) {
        preferences
                .edit()
                .putFloat(LATITUDE_KEY, (float) center.latitude)
                .putFloat(LONGITUDE_KEY, (float) center.longitude)
                .apply();
    }

    public int getNotificationAreaRadius() {
        return preferences.getInt(RADIUS_KEY, DEFAULT_RADIUS_METERS);
    }

    public void setNotificationAreaRadius(int radius) {
        preferences.edit().putInt(RADIUS_KEY, radius).apply();
    }

    public boolean shouldNotifyUser(Merchant merchant) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.getBoolean(context.getString(R.string.pref_show_new_merchants_key), true)) {
            return false;
        }

        LatLng notificationAreaCenter = getNotificationAreaCenter();

        if (notificationAreaCenter == null) {
            return false;
        }

        float[] distance = new float[1];
        Location.distanceBetween(notificationAreaCenter.latitude, notificationAreaCenter.longitude, merchant.getLatitude(), merchant.getLongitude(), distance);
        Log.d(TAG, "Distance: " + distance[0]);

        if (distance[0] > getNotificationAreaRadius()) {
            return false;
        }

        App app = (App) context.getApplicationContext();
        SQLiteDatabase db = app.getDatabaseHelper().getReadableDatabase();

        Cursor cursor = db.query(Database.Merchants.TABLE_NAME,
                new String[] { Database.Merchants._ID },
                "_id = ?",
                new String[] { String.valueOf(merchant.getId()) },
                null,
                null,
                null);

        boolean alreadyExists = cursor.getCount() > 0;
        cursor.close();

        if (alreadyExists) {
            return false;
        }

        return true;
    }

    public void notifyUser(long merchantId, String merchantName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_new_merchant))
                .setContentText(!TextUtils.isEmpty(merchantName) ? merchantName : context.getString(R.string.notification_new_merchant_no_name))
                .setAutoCancel(true);

        Intent intent = MapActivity.newShowMerchantIntent(context, merchantId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(UUID.randomUUID().hashCode(), builder.build());
    }
}