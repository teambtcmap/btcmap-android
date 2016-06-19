package com.bubelov.coins.api.external;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 21:43
 */

public interface BitcoinAverageApi {
    @GET("ticker/global/USD")
    Call<BitcoinAverageTickerResponse> getUsdTicker();
}
