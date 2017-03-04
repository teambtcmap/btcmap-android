package com.bubelov.coins.util;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.User;
import com.google.gson.Gson;

import javax.inject.Inject;

/**
 * @author Igor Bubelov
 */

public class AuthController {
    @Inject
    SharedPreferences preferences;

    @Inject
    Gson gson;

    public AuthController() {
        Injector.INSTANCE.getAndroidComponent().inject(this);
    }

    public @Nullable User getUser() {
        return gson.fromJson(preferences.getString(PreferenceKeys.USER, null), User.class);
    }

    public void setUser(@Nullable User user) {
        preferences.edit().putString(PreferenceKeys.USER, gson.toJson(user)).apply();
    }

    public @NonNull String getToken() {
        return preferences.getString(PreferenceKeys.API_AUTH_TOKEN, "");
    }

    public void setToken(@Nullable String token) {
        preferences.edit().putString(PreferenceKeys.API_AUTH_TOKEN, token).apply();
    }

    public @NonNull String getMethod() {
        return preferences.getString(PreferenceKeys.API_AUTH_METHOD, "");
    }

    public void setMethod(@Nullable String method) {
        preferences.edit().putString(PreferenceKeys.API_AUTH_METHOD, method).apply();
    }

    public boolean isAuthorized() {
        return !TextUtils.isEmpty(getToken());
    }
}