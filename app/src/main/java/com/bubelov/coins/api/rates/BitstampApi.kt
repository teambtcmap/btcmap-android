package com.bubelov.coins.api.rates

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface BitstampApi {
    @GET("ticker/")
    fun getTicker(): Call<BitstampTicker>
}