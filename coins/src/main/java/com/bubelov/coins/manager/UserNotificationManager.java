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
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Tables;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.ui.activity.MapActivity;
import com.google.android.gms.maps.model.LatLng;

/**
 * Author: Igor Bubelov
 * Date: 12/07/14 14:46
 */

public class UserNotificationManager {
    private static final String TAG = UserNotificationManager.class.getName();

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
                .commit();
    }

    public Integer getNotificationAreaRadius() {
        if (preferences.contains(RADIUS_KEY)) {
            return preferences.getInt(RADIUS_KEY, -1);
        } else {
            return null;
        }
    }

    public void setNotificationAreaRadius(int radius) {
        preferences.edit().putInt(RADIUS_KEY, radius).commit();
    }

    public void onMerchantDownload(Merchant merchant) {
        if (new MerchantSyncManager(context).getLastSyncMillis() == 0) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.getBoolean(context.getString(R.string.pref_show_new_merchants_key), true)) {
            return;
        }

        LatLng notificationAreaCenter = getNotificationAreaCenter();

        if (getNotificationAreaCenter() == null) {
            return;
        }

        float[] distance = new float[1];
        Location.distanceBetween(notificationAreaCenter.latitude, notificationAreaCenter.longitude, merchant.getLatitude(), merchant.getLongitude(), distance);
        Log.d(TAG, "Distance: " + distance[0]);

//        if (distance[0] > getNotificationAreaRadius()) {
//            return;
//        }

        App app = (App) context.getApplicationContext();
        SQLiteDatabase db = app.getDatabaseHelper().getReadableDatabase();

        Cursor cursor = db.query(Tables.Merchants.TABLE_NAME,
                new String[]{Tables.Merchants._ID},
                "_id = ?",
                new String[]{String.valueOf(merchant.getId())},
                null,
                null,
                null);

        boolean alreadyExists = cursor.getCount() > 0;
        cursor.close();

        if (alreadyExists) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_new_merchant))
                .setContentText(!TextUtils.isEmpty(merchant.getName()) ? merchant.getName() : context.getString(R.string.notification_new_merchant_no_name))
                .setAutoCancel(true);

        Intent resultIntent = MapActivity.newShowMerchantIntent(context, merchant.getLatitude(), merchant.getLongitude());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MapActivity.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent((int)merchant.getId(), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int)merchant.getId(), builder.build());
    }
}
