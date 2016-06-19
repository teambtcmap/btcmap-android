package com.bubelov.coins.util;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/7/15 9:24 AM
 */

public class ExchangeRatesFactory {
    public static ExchangeRate newExchangeRate(Currency sourceCurrency, Currency targetCurrency, float value) {
        ExchangeRate rate = new ExchangeRate();
        rate.setSourceCurrencyId(sourceCurrency.getId());
        rate.setTargetCurrencyId(targetCurrency.getId());
        rate.setValue(value);
        long now = System.currentTimeMillis();
        rate.setCreatedAt(new DateTime(now));
        rate.setUpdatedAt(new DateTime(now));
        return rate;
    }
}