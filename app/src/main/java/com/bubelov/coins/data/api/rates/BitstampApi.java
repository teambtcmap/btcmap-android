package com.bubelov.coins.data.api.rates;

import com.bubelov.coins.data.api.rates.model.BitstampTicker;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 */

public interface BitstampApi {
    @GET("ticker/")
    Call<BitstampTicker> getTicker();
}