package com.bubelov.coins.data.api.rates;

import com.bubelov.coins.data.api.rates.model.BitcoinAverageTickerResponse;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Author: Igor Bubelov
 */

public interface BitcoinAverageApi {
    @GET("ticker/global/USD")
    Call<BitcoinAverageTickerResponse> getUsdTicker();
}