package com.bubelov.coins.api.rates

/**
 * @author Igor Bubelov
 */

class CoinbaseResponse {
    var data: CoinbaseResponseData? = null

    inner class CoinbaseResponseData {
        var currency: String? = null
        var rates: Map<String, Double>? = null
    }
}