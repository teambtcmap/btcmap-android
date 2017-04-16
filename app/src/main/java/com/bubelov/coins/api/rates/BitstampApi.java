package com.bubelov.coins.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 */

public interface BitstampApi {
    @GET("ticker/")
    Call<BitstampTicker> getTicker();
}