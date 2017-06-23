package com.bubelov.coins.api.rates

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface CoinbaseApi {
    @GET("exchange-rates?currency=BTC")
    fun getExchangeRates(): Call<CoinbaseResponse>
}