package com.bubelov.coins.repository.rate

import com.bubelov.coins.model.CurrencyPair
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
    private val sources = listOf(bitcoinAverage, bitstamp, coinbase, winkdex)

    fun getExchangeRatesSources(currencyPair: CurrencyPair)
            = sources.filter { it.getCurrencyPairs().contains(currencyPair) }
}