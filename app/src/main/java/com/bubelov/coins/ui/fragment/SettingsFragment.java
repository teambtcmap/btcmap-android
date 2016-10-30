package com.bubelov.coins.ui.fragment;

import android.content.SharedPreferences;
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
import com.bubelov.coins.service.rates.ExchangeRatesService;
import com.bubelov.coins.service.sync.merchants.DatabaseSyncService;
import com.bubelov.coins.service.sync.merchants.UserNotificationController;
import com.bubelov.coins.ui.activity.CurrenciesActivity;
import com.bubelov.coins.ui.activity.NotificationAreaActivity;

import java.util.Random;

/**
 * @author Igor Bubelov
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
            NotificationAreaActivity.start(getActivity());
        }

        if (preference.getKey().equals(getString(R.string.pref_currencies_key))) {
            CurrenciesActivity.start(getActivity());
        }

        if (preference.getKey().equals("pref_test_notification")) {
            SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

            Cursor cursor = db.rawQuery("select count(_id) from " + DbContract.Merchants.TABLE_NAME, null);

            if (cursor.moveToNext()) {
                int merchantsCount = cursor.getInt(0);
                cursor.close();

                Random random = new Random(System.currentTimeMillis());

                cursor = db.query(DbContract.Merchants.TABLE_NAME,
                        new String[]{DbContract.Merchants._ID, DbContract.Merchants.NAME},
                        "_id = ?",
                        new String[]{String.valueOf(random.nextInt(merchantsCount + 1))},
                        null,
                        null,
                        null);

                if (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(DbContract.Merchants._ID));
                    String name = cursor.getString(cursor.getColumnIndex(DbContract.Merchants.NAME));
                    new UserNotificationController(getActivity()).notifyUser(id, name);
                }

                cursor.close();
            }
        }

        if (preference.getKey().equals("pref_update_merchants")) {
            DatabaseSyncService.start(getActivity());
        }

        if (preference.getKey().equals("pref_remove_last_merchant")) {
            SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

            Cursor cursor = db.query(DbContract.Merchants.TABLE_NAME, new String[]{DbContract.Merchants._ID}, null, null, null, null, DbContract.Merchants._UPDATED_AT + " DESC", "1");

            if (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                cursor.close();

                int rowsAffected = db.delete(DbContract.Merchants.TABLE_NAME, DbContract.Merchants._ID + " = ?", new String[]{String.valueOf(id)});

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
        if (key.equals(getString(R.string.pref_exchange_rates_provider_key))) {
            getActivity().startService(ExchangeRatesService.newIntent(getActivity(),
                    "BTC",
                    "USD",
                    true));
        }
    }
}