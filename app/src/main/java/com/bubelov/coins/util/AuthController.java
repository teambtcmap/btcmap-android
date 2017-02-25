package com.bubelov.coins.util;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.model.User;
import com.google.gson.Gson;

import javax.inject.Inject;

/**
 * @author Igor Bubelov
 */

public class AuthController {
    private static final String USER = "user";

    private static final String TOKEN = "token";

    @Inject
    SharedPreferences preferences;

    @Inject
    Gson gson;

    public AuthController() {
        Injector.INSTANCE.getAndroidComponent().inject(this);
    }

    public @Nullable User getUser() {
        return gson.fromJson(preferences.getString(USER, null), User.class);
    }

    public void setUser(@Nullable User user) {
        preferences.edit().putString(USER, gson.toJson(user)).apply();
    }

    public @NonNull String getToken() {
        return preferences.getString(TOKEN, "");
    }

    public void setToken(@Nullable String token) {
        preferences.edit().putString(TOKEN, token).apply();
    }

    public boolean isAuthorized() {
        return !TextUtils.isEmpty(getToken());
    }
}