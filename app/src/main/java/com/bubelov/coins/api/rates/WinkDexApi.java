package com.bubelov.coins.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:41
 */

public interface WinkDexApi {
    @GET("price")
    Call<WinkDexPriceResponse> getPrice();
}