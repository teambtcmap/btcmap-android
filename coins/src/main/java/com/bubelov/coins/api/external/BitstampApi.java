package com.bubelov.coins.api.external;

import retrofit.Call;
import retrofit.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 9/28/15 10:03 PM
 */

public interface BitstampApi {
    @GET("ticker/")
    Call<BitstampTicker> getTicker();
}
