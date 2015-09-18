package com.bubelov.coins.api.external;

import retrofit.Call;
import retrofit.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 08/05/15 19:41
 */

public interface WinkDexApi {
    @GET("price")
    Call<WinkDexPriceResponse> getPrice();
}
