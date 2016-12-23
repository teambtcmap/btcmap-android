package com.bubelov.coins.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.bubelov.coins.dagger.Injector;

/**
 * @author Igor Bubelov
 */

public class AuthUtils {
    private static final String AUTH_TOKEN = "auth_token";

    public static String getToken() {
        return getPreferences().getString(AUTH_TOKEN, "");
    }

    public static void setToken(String token) {
        getPreferences().edit().putString(AUTH_TOKEN, token).apply();
    }

    public static boolean isAuthorized() {
        return !TextUtils.isEmpty(getToken());
    }

    private static SharedPreferences getPreferences() {
        Context context = Injector.INSTANCE.getAppComponent().getContext();
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
