package com.bubelov.coins.data.api.rates;

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