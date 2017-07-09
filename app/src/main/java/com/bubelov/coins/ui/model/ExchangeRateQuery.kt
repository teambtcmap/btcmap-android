package com.bubelov.coins.ui.model

/**
 * @author Igor Bubelov
 */

sealed class ExchangeRateQuery {
    data class ExchangeRate(
            val source: String,
            val currencyCode: String,
            val rate: Double
    ) : ExchangeRateQuery()

    data class Loading(val source: String) : ExchangeRateQuery()
    data class Error(val source: String) : ExchangeRateQuery()
}