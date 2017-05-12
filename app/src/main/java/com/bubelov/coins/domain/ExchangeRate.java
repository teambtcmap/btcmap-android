package com.bubelov.coins.domain;

/**
 * @author Igor Bubelov
 */

public class ExchangeRate {
    public String source;

    public String baseCurrencyCode;

    public String targetCurrencyCode;

    public double rate;

    public ExchangeRate(String source, String baseCurrencyCode, String targetCurrencyCode, double rate) {
        this.source = source;
        this.baseCurrencyCode = baseCurrencyCode;
        this.targetCurrencyCode = targetCurrencyCode;
        this.rate = rate;
    }
}