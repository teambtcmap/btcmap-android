package com.bubelov.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.util.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class PlaceCategory implements Parcelable {
    public abstract long id();
    public abstract String name();

    public static Builder builder() {
        return new AutoValue_PlaceCategory.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder name(String name);
        public abstract PlaceCategory build();
    }
}
