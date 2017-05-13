package com.bubelov.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.util.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class Currency implements Parcelable {
    public abstract long id();
    public abstract String name();
    public abstract String code();
    public abstract boolean crypto();

    public static Builder builder() {
        return new AutoValue_Currency.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder name(String name);
        public abstract Builder code(String code);
        public abstract Builder crypto(boolean crypto);
        public abstract Currency build();
    }
}