package com.bubelov.coins.repository.rate

import com.bubelov.coins.model.CurrencyPair
import com.bubelov.coins.repository.Result

/**
 * @author Igor Bubelov
 */

interface ExchangeRatesSource {
    val name: String

    fun getCurrencyPairs(): Collection<CurrencyPair>

    fun getExchangeRate(pair: CurrencyPair): Result<Double>
}