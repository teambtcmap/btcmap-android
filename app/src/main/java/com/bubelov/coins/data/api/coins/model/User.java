package com.bubelov.coins.data.api.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.data.gson.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class User implements Parcelable {
    public abstract long id();
    public abstract String email();
    public abstract String firstName();
    public abstract String lastName();
    public abstract String avatarUrl();

    public static Builder builder() {
        return new AutoValue_User.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder email(String email);
        public abstract Builder firstName(String firstName);
        public abstract Builder lastName(String lastName);
        public abstract Builder avatarUrl(String avatarUrl);
        public abstract User build();
    }
}