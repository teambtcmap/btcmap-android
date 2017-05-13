package com.bubelov.coins.model;

import com.bubelov.coins.util.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class PlaceNotification {
    public abstract long placeId();
    public abstract String placeName();

    public static Builder builder() {
        return new AutoValue_PlaceNotification.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder placeId(long id);
        public abstract Builder placeName(String name);
        public abstract PlaceNotification build();
    }
}