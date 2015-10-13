package com.bubelov.coins.model;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 8:29 PM
 */

public class ExchangeRate extends AbstractEntity {
    private long sourceCurrencyId;

    private long targetCurrencyId;

    private float value;

    public long getSourceCurrencyId() {
        return sourceCurrencyId;
    }

    public void setSourceCurrencyId(long sourceCurrencyId) {
        this.sourceCurrencyId = sourceCurrencyId;
    }

    public long getTargetCurrencyId() {
        return targetCurrencyId;
    }

    public void setTargetCurrencyId(long targetCurrencyId) {
        this.targetCurrencyId = targetCurrencyId;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}