package com.bubelov.coins.repository.user;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.api.coins.CoinsApi;
import com.bubelov.coins.api.coins.AuthResponse;
import com.bubelov.coins.api.coins.NewUserParams;
import com.bubelov.coins.model.User;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;
import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class UserRepository {
    private CoinsApi api;

    private SharedPreferences preferences;

    private Gson gson;

    @Inject
    UserRepository(CoinsApi api, SharedPreferences preferences, Gson gson) {
        this.api = api;
        this.preferences = preferences;
        this.gson = gson;
    }

    public User getUser() {
        return gson.fromJson(preferences.getString(PreferenceKeys.USER, null), User.class);
    }

    public void setUser(User user) {
        preferences.edit().putString(PreferenceKeys.USER, gson.toJson(user)).apply();
    }

    public String getUserAuthToken() {
        return preferences.getString(PreferenceKeys.API_AUTH_TOKEN, "");
    }

    public void setUserAuthToken(String token) {
        preferences.edit().putString(PreferenceKeys.API_AUTH_TOKEN, token).apply();
    }

    public String getUserAuthMethod() {
        return preferences.getString(PreferenceKeys.API_AUTH_METHOD, "");
    }

    public void setUserAuthMethod(String method) {
        preferences.edit().putString(PreferenceKeys.API_AUTH_METHOD, method).apply();
    }

    public boolean signIn(String googleToken) {
        Response<AuthResponse> response = null;

        try {
            response = api.authWithGoogle(googleToken).execute();
        } catch (IOException e) {
            Timber.e(e, "Couldn't authorize with Google token");
            FirebaseCrash.report(e);
        }

        if (response == null) {
            return false;
        }

        if (response.isSuccessful()) {
            setUser(response.body().getUser());
            setUserAuthToken(response.body().getToken());
            setUserAuthMethod("google");
            onAuthorized();
            return true;
        } else {
            return false;
        }
    }

    public boolean signIn(String email, String password) {
        Response<AuthResponse> response = null;

        try {
            response = api.authWithEmail(email, password).execute();
        } catch (IOException e) {
            Timber.e(e, "Couldn't authorize with email");
            FirebaseCrash.report(e);
        }

        if (response == null) {
            return false;
        }

        if (response.isSuccessful()) {
            setUser(response.body().getUser());
            setUserAuthToken(response.body().getToken());
            setUserAuthMethod("email");
            onAuthorized();
            return true;
        } else {
            return false;
        }
    }

    public boolean signUp(String email, String password, String firstName, String lastName) {
        Response<AuthResponse> response = null;

        try {
            response = api.createUser(new NewUserParams(email, password, firstName, lastName)).execute();
        } catch (IOException e) {
            Timber.e(e, "Couldn't sign up");
            FirebaseCrash.report(e);
        }

        if (response == null) {
            return false;
        }

        if (response.isSuccessful()) {
            setUser(response.body().getUser());
            setUserAuthToken(response.body().getToken());
            setUserAuthMethod("email");
            onAuthorized();
            return true;
        } else {
            return false;
        }
    }

    private void onAuthorized() {
        FirebaseAnalytics analytics = Injector.INSTANCE.mainComponent().analytics();
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, getUserAuthMethod());
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle);
    }
}