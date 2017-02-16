package com.bubelov.coins.model;

/**
 * @author Igor Bubelov
 */

public class ExchangeRate extends AbstractEntity {
    private String currency;

    private String baseCurrency;

    private double value;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}