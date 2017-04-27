package com.bubelov.coins.data.api.rates.model;

import java.util.Map;

/**
 * @author Igor Bubelov
 */

public class CoinbaseResponse {
    public CoinbaseResponseData data;

    public class CoinbaseResponseData {
        public String currency;

        public Map<String, Double> rates;
    }
}