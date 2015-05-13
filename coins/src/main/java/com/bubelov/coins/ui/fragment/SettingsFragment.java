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

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.manager.MerchantSyncManager;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.ui.activity.SelectAreaActivity;

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

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_sync_merchants_key))) {
            MerchantSyncManager syncManager = new MerchantSyncManager(getActivity());
            syncManager.scheduleAlarm();
        }
    }
}