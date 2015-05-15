package com.bubelov.coins.ui.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.ActivityOptionsCompat;
import android.widget.Toast;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.manager.DatabaseSyncManager;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.ui.activity.SelectAreaActivity;

import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * Author: Igor Bubelov
 * Date: 11/07/14 20:31
 */

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        DatabaseSyncManager syncManager = new DatabaseSyncManager(getActivity());
        getPreferenceManager().findPreference("pref_update_merchants").setSummary("Last sync: " + SimpleDateFormat.getDateTimeInstance().format(syncManager.getLastSyncMillis()));
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(getString(R.string.pref_area_of_interest_key))) {
            startActivity(new Intent(getActivity(), SelectAreaActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle());
        }

        if (preference.getKey().equals("pref_test_notification")) {
            SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

            Cursor cursor = db.rawQuery("select count(_id) from " + Database.Merchants.TABLE_NAME, null);

            if (cursor.moveToNext()) {
                int merchantsCount = cursor.getInt(0);
                cursor.close();

                Random random = new Random(System.currentTimeMillis());

                cursor = db.query(Database.Merchants.TABLE_NAME,
                        new String[] { Database.Merchants._ID, Database.Merchants.NAME },
                        "_id = ?",
                        new String[] { String.valueOf(random.nextInt(merchantsCount + 1)) },
                        null,
                        null,
                        null);

                if (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(Database.Merchants._ID));
                    String name = cursor.getString(cursor.getColumnIndex(Database.Merchants.NAME));
                    new UserNotificationManager(getActivity()).notifyUser(id, name);
                }

                cursor.close();
            }
        }

        if (preference.getKey().equals("pref_update_merchants")) {
            new DatabaseSyncManager(getActivity()).sheduleDelayed(0);
        }

        if (preference.getKey().equals("pref_remove_last_merchant")) {
            SQLiteDatabase db = App.getInstance().getDatabaseHelper().getWritableDatabase();

            Cursor cursor = db.query(Database.Merchants.TABLE_NAME, new String[] { Database.Merchants._ID }, null, null, null, null, Database.Merchants._UPDATED_AT + " DESC", "1");

            if (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                cursor.close();

                int rowsAffected = db.delete(Database.Merchants.TABLE_NAME, Database.Merchants._ID + " = ?", new String[] { String.valueOf(id) } );

                if (rowsAffected > 0) {
                    Toast.makeText(getActivity(), "Removed!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Couldn't remove merchant", Toast.LENGTH_SHORT).show();
                }
            } else {
                cursor.close();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_sync_merchants_key))) {
            DatabaseSyncManager syncManager = new DatabaseSyncManager(getActivity());
            syncManager.scheduleAlarm();
        }
    }
}