package com.bubelov.coins.api;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Author: Igor Bubelov
 * Date: 17/04/15 10:42
 */

public interface CoinsApi {
    @GET("merchants")
    Call<List<Merchant>> getMerchants(@Query("since") String since, @Query("limit") int limit);

    @GET("currencies")
    Call<List<Currency>> getCurrencies();

    @POST("place_suggestions")
    Call<Void> addPlaceSuggestion(@Query("message") String message);
}
