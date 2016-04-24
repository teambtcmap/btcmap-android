package com.bubelov.coins.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.dagger.Injector;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 24/04/16 12:21
 */

public class MerchantNotification {
    private static final String KEY_PENDING_MERCHANTS = "pending_merchants_v2";

    private long merchantId;

    public long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(long merchantId) {
        this.merchantId = merchantId;
    }

    public static List<MerchantNotification> queryForAll() {
        Context context = Injector.INSTANCE.getAppComponent().getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.contains(KEY_PENDING_MERCHANTS)) {
            TypeToken typeToken = new TypeToken<ArrayList<MerchantNotification>>(){};
            return new Gson().fromJson(preferences.getString(KEY_PENDING_MERCHANTS, "[]"), typeToken.getType());
        } else {
            return new ArrayList<>();
        }
    }

    public static void insert(MerchantNotification merchant) {
        List<MerchantNotification> allMerchants = queryForAll();
        allMerchants.add(merchant);
        replaceWith(allMerchants);
    }

    public static void deleteAll() {
        Context context = Injector.INSTANCE.getAppComponent().getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(KEY_PENDING_MERCHANTS).apply();
    }

    private static void replaceWith(List<MerchantNotification> merchants) {
        Context context = Injector.INSTANCE.getAppComponent().getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(KEY_PENDING_MERCHANTS, new Gson().toJson(merchants)).apply();
    }
}