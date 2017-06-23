package com.bubelov.coins.api.rates

import retrofit2.Call
import retrofit2.http.GET

/**
 * @author Igor Bubelov
 */

interface WinkDexApi {
    @GET("price")
    fun getPrice(): Call<WinkDexPriceResponse>
}