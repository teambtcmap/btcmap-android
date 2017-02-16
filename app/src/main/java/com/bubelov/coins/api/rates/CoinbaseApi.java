package com.bubelov.coins.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 9/28/15 9:26 PM
 */

public interface CoinbaseApi {
    @GET("products/BTC-USD/ticker")
    Call<CoinbaseTicker> getTicker();
}