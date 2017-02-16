package com.bubelov.coins.ui.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.bubelov.coins.R;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.service.DatabaseSyncService;
import com.bubelov.coins.service.UserNotificationController;
import com.bubelov.coins.ui.activity.NotificationAreaActivity;

import java.util.Random;

/**
 * @author Igor Bubelov
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(getString(R.string.pref_area_of_interest_key))) {
            NotificationAreaActivity.start(getActivity());
        }

        if (preference.getKey().equals("pref_test_notification")) {
            SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

            Cursor cursor = db.rawQuery("select count(_id) from " + DbContract.Places.TABLE_NAME, null);

            if (cursor.moveToNext()) {
                int placesCount = cursor.getInt(0);
                cursor.close();

                Random random = new Random(System.currentTimeMillis());

                cursor = db.query(DbContract.Places.TABLE_NAME,
                        new String[]{DbContract.Places._ID, DbContract.Places.NAME},
                        "_id = ?",
                        new String[]{String.valueOf(random.nextInt(placesCount + 1))},
                        null,
                        null,
                        null);

                if (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(DbContract.Places._ID));
                    String name = cursor.getString(cursor.getColumnIndex(DbContract.Places.NAME));
                    new UserNotificationController(getActivity()).notifyUser(id, name);
                }

                cursor.close();
            }
        }

        if (preference.getKey().equals("pref_update_places")) {
            DatabaseSyncService.start(getActivity());
        }

        if (preference.getKey().equals("pref_remove_last_place")) {
            SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

            Cursor cursor = db.query(DbContract.Places.TABLE_NAME, new String[]{DbContract.Places._ID}, null, null, null, null, DbContract.Places._UPDATED_AT + " DESC", "1");

            if (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                cursor.close();

                int rowsAffected = db.delete(DbContract.Places.TABLE_NAME, DbContract.Places._ID + " = ?", new String[]{String.valueOf(id)});

                if (rowsAffected > 0) {
                    Toast.makeText(getActivity(), "Removed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Couldn't remove place", Toast.LENGTH_SHORT).show();
                }
            } else {
                cursor.close();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}