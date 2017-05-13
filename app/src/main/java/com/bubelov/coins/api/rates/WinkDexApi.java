package com.bubelov.coins.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * @author Igor Bubelov
 */

public interface WinkDexApi {
    @GET("price")
    Call<WinkDexPriceResponse> getPrice();
}