package com.bubelov.coins.api.rates

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface CoinbaseApi {
    @GET("exchange-rates?currency=BTC")
    fun getExchangeRates(): Call<Response>
}

data class Response (val data: Data)

data class Data (val currency: String, val rates: Map<String, Double>)