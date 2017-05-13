package com.bubelov.coins.model;

import android.os.Parcelable;

import com.bubelov.coins.util.AutoGson;
import com.google.auto.value.AutoValue;

/**
 * @author Igor Bubelov
 */

@AutoValue
@AutoGson
public abstract class ExchangeRate implements Parcelable {
    public abstract long id();
    public abstract String source();
    public abstract String baseCurrencyCode();
    public abstract String targetCurrencyCode();
    public abstract double rate();
    public abstract long date();

    public static Builder builder() {
        return new AutoValue_ExchangeRate.Builder();
    }

    @AutoValue.Builder public static abstract class Builder {
        public abstract Builder id(long id);
        public abstract Builder source(String source);
        public abstract Builder baseCurrencyCode(String code);
        public abstract Builder targetCurrencyCode(String code);
        public abstract Builder rate(double rate);
        public abstract Builder date(long date);
        public abstract ExchangeRate build();
    }
}