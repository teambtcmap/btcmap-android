package com.bubelov.coins.api.rates.provider;

import com.bubelov.coins.model.ExchangeRate;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:34 PM
 */

public interface ExchangeRatesProvider {
    ExchangeRate getExchangeRate(String currency, String baseCurrency) throws Exception;
}