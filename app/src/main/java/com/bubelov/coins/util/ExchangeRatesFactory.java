package com.bubelov.coins.util;

import com.bubelov.coins.model.ExchangeRate;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/7/15 9:24 AM
 */

public class ExchangeRatesFactory {
    public static ExchangeRate newExchangeRate(String currency, String baseCurrency, float value) {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrency(currency);
        rate.setBaseCurrency(baseCurrency);
        rate.setValue(value);
        long now = System.currentTimeMillis();
        rate.setCreatedAt(new DateTime(now));
        rate.setUpdatedAt(new DateTime(now));
        return rate;
    }
}