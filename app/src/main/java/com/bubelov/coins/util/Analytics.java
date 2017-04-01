package com.bubelov.coins.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bubelov.coins.dagger.Injector;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * @author Igor Bubelov
 */

public class Analytics {
    public static void logSelectContentEvent(@NonNull String itemId, @Nullable String itemName, @NonNull String contentType) {
        logContentEvent(FirebaseAnalytics.Event.SELECT_CONTENT, itemId, itemName, contentType);
    }

    public static void logShareContentEvent(@NonNull String itemId, @Nullable String itemName, @NonNull String contentType) {
        logContentEvent(FirebaseAnalytics.Event.SHARE, itemId, itemName, contentType);
    }

    private static void logContentEvent(@NonNull String eventType, @NonNull String itemId, @Nullable String itemName, @NonNull String contentType) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);

        if (TextUtils.isEmpty(itemName)) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, itemName);
        }

        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, contentType);
        Injector.INSTANCE.mainComponent().analytics().logEvent(eventType, bundle);
    }
}