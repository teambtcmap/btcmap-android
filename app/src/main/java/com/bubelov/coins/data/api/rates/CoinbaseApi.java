package com.bubelov.coins.data.api.rates;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * @author Igor Bubelov
 */

public interface CoinbaseApi {
    @GET("exchange-rates?currency=BTC")
    Call<CoinbaseResponse> getExchangeRates();
}