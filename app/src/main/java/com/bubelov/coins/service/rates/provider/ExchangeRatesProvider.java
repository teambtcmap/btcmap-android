package com.bubelov.coins.service.rates.provider;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 6:34 PM
 */

public interface ExchangeRatesProvider {
    ExchangeRate getExchangeRate(Currency sourceCurrency, Currency targetCurrency) throws Exception;
}