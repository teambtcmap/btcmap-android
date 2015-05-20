package com.bubelov.coins.api.external;

import retrofit.http.GET;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 21:43
 */

public interface BitcoinAverageApi {
    @GET("/ticker/global/USD/")
    BitcoinAverageTickerResponse getUsdTicker();
}
