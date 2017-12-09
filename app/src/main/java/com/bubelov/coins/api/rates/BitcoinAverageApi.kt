package com.bubelov.coins.api.rates

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface BitcoinAverageApi {
    @GET("indices/global/ticker/short?crypto=BTC&fiat=USD")
    fun getUsdTicker(): Call<JsonObject>
}