package com.bubelov.coins.repository.rate

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class ExchangeRatesRepository @Inject constructor(
        bitcoinAverage: BitcoinAverage,
        bitstamp: Bitstamp,
        coinbase: Coinbase,
        winkdex: Winkdex
) {
    private val sources = listOf(bitstamp, coinbase, winkdex)

    fun getExchangeRatesSources(baseCurrency: String, targetCurrency: String)
            = sources.filter { it.getSupportedCurrencyPairs().contains(Pair(baseCurrency, targetCurrency)) }
}