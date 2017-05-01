package com.bubelov.coins.data.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * @author Igor Bubelov
 */

public interface BitstampApi {
    @GET("ticker/")
    Call<BitstampTicker> getTicker();
}