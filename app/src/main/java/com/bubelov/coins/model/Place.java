package com.bubelov.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.util.AutoGson;
import com.google.android.gms.maps.model.LatLng;
import com.google.auto.value.AutoValue;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Collection;
import java.util.Date;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class Place implements Parcelable, ClusterItem {
    public abstract long id();
    public abstract String name();
    public abstract String description();
    public abstract double latitude();
    public abstract double longitude();
    public abstract long categoryId();
    public abstract String phone();
    public abstract String website();
    public abstract String openingHours();
    public abstract boolean visible();
    public abstract int openedClaims();
    public abstract int closedClaims();
    public abstract Date updatedAt();

    public Collection<Currency> currencies;

    public static Builder builder() {
        return new AutoValue_Place.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder name(String name);
        public abstract Builder description(String description);
        public abstract Builder latitude(double latitude);
        public abstract Builder longitude(double longitude);
        public abstract Builder categoryId(long categoryId);
        public abstract Builder phone(String phone);
        public abstract Builder website(String website);
        public abstract Builder openingHours(String openingHours);
        public abstract Builder visible(boolean visible);
        public abstract Builder openedClaims(int openedClaims);
        public abstract Builder closedClaims(int closedClaims);
        public abstract Builder updatedAt(Date updatedAt);
        public abstract Place build();
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(latitude(), longitude());
    }
}