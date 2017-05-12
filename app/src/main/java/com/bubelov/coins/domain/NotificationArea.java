package com.bubelov.coins.domain;

import android.os.Parcelable;

import com.bubelov.coins.util.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class NotificationArea implements Parcelable {
    public abstract double latitude();
    public abstract double longitude();
    public abstract double radius();

    public static Builder builder() {
        return new AutoValue_NotificationArea.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder latitude(double latitude);
        public abstract Builder longitude(double longitude);
        public abstract Builder radius(double radius);
        public abstract NotificationArea build();
    }
}