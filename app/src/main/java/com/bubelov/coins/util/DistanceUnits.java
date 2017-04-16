package com.bubelov.coins.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Author: Igor Bubelov
 */

public enum DistanceUnits {
    KILOMETERS,
    MILES;

    public static DistanceUnits getDefault() {
        return getFrom(Locale.getDefault());
    }

    private static DistanceUnits getFrom(Locale locale) {
        Set<String> mileUsingCountries = new HashSet<>();
        mileUsingCountries.add("LR"); // Liberia
        mileUsingCountries.add("MM"); // Myanmar
        mileUsingCountries.add("GB"); // United Kingdom
        mileUsingCountries.add("US"); // United States

        if (mileUsingCountries.contains(locale.getCountry())) {
            return MILES;
        } else {
            return KILOMETERS;
        }
    }
}