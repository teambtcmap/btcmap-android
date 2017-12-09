package com.bubelov.coins.model

/**
 * @author Igor Bubelov
 */

enum class CurrencyPair(data: Pair<String, String>) {
    BTC_USD(Pair("BTC", "USD")),
    BTC_EUR(Pair("BTC", "EUR")),
    BTC_GBP(Pair("BTC", "GBP"));

    val displayCurrency = data.second
}