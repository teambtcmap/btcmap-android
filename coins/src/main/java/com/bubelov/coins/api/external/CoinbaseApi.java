package com.bubelov.coins.api.external;

import retrofit.Call;
import retrofit.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 9/28/15 9:26 PM
 */

public interface CoinbaseApi {
    @GET("products/BTC-USD/ticker")
    Call<CoinbaseTicker> getTicker();
}