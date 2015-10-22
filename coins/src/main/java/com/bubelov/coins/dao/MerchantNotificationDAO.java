package com.bubelov.coins.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 10/22/15 11:08 AM
 */

public class MerchantNotificationDAO {
    private static final String KEY_PENDING_MERCHANTS = "pending_merchants";

    private SharedPreferences preferences;

    public MerchantNotificationDAO(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List<String> queryForAll() {
        if (preferences.contains(KEY_PENDING_MERCHANTS)) {
            TypeToken typeToken = new TypeToken<ArrayList<String>>(){};
            return new Gson().fromJson(preferences.getString(KEY_PENDING_MERCHANTS, "[]"), typeToken.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public void insert(String merchant) {
        List<String> allMerchants = queryForAll();
        allMerchants.add(merchant);
        replaceWith(allMerchants);
    }

    public void deleteAll() {
        preferences.edit().remove(KEY_PENDING_MERCHANTS).apply();
    }

    private void replaceWith(List<String> merchants) {
        preferences.edit().putString(KEY_PENDING_MERCHANTS, new Gson().toJson(merchants)).apply();
    }
}