package com.bubelov.coins.data.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * @author Igor Bubelov
 */

public interface BitcoinAverageApi {
    @GET("ticker/global/USD")
    Call<BitcoinAverageTickerResponse> getUsdTicker();
}