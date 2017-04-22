package com.bubelov.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.gson.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * Author: Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class Currency2 implements Parcelable {
    public abstract long id();
    public abstract String name();
    public abstract String code();
    public abstract boolean crypto();

    public static Builder builder() {
        return new AutoValue_Currency2.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder name(String name);
        public abstract Builder code(String code);
        public abstract Builder crypto(boolean crypto);
        public abstract Currency2 build();
    }
}