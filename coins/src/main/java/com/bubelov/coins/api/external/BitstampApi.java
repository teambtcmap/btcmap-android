package com.bubelov.coins.api.external;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 9/28/15 10:03 PM
 */

public interface BitstampApi {
    @GET("ticker/")
    Call<BitstampTicker> getTicker();
}
