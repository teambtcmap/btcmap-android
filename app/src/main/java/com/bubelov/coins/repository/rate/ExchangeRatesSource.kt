package com.bubelov.coins.repository.rate

import com.bubelov.coins.model.ExchangeRate

/**
 * @author Igor Bubelov
 */

interface ExchangeRatesSource {
    val name: String

    fun getSupportedCurrencyPairs(): Collection<Pair<String, String>>

    fun getExchangeRate(baseCurrency: String, targetCurrency: String): ExchangeRate
}