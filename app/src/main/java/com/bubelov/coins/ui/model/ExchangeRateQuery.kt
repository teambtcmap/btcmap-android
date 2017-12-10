package com.bubelov.coins.ui.model

/**
 * @author Igor Bubelov
 */

sealed class ExchangeRateQuery {
    data class Loading(val source: String) : ExchangeRateQuery()
    data class Success(val source: String, val rate: String) : ExchangeRateQuery()
    data class Error(val source: String) : ExchangeRateQuery()
}