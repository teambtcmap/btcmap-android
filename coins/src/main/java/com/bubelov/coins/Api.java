package com.bubelov.coins;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Author: Igor Bubelov
 * Date: 17/04/15 10:42
 */

public interface Api {
    @GET("/merchants")
    List<Merchant> getMerchants(@Query("currency") String currencyCode, @Query("page") int page, @Query("per_page") int perPage);

    @GET("/currencies")
    List<Currency> getCurrencies();
}
