package com.bubelov.coins.api.rates

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface BitcoinAverageApi {
    @GET("ticker/global/USD")
    fun getUsdTicker(): Call<BitcoinAverageTickerResponse>
}