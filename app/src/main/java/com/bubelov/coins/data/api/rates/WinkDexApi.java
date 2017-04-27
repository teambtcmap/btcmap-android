package com.bubelov.coins.data.api.rates;

import com.bubelov.coins.data.api.rates.model.WinkDexPriceResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 */

public interface WinkDexApi {
    @GET("price")
    Call<WinkDexPriceResponse> getPrice();
}