package com.bubelov.coins.model

/**
 * @author Igor Bubelov
 */

data class ExchangeRate(
        val id: Long,
        val source: String,
        val baseCurrencyCode: String,
        val targetCurrencyCode: String,
        val rate: Double,
        val date: Long
)