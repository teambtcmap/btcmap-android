package com.bubelov.coins.api;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.SessionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * @author Igor Bubelov
 */

public interface CoinsApi {
    @POST("auth/google-token")
    Call<SessionResponse> getSession(@Header("token") String token);

    @GET("merchants")
    Call<List<Place>> getPlaces(@Query("since") String since, @Query("limit") int limit);

    @GET("currencies")
    Call<List<Currency>> getCurrencies();

    @POST("place_suggestions")
    Call<Void> addPlaceSuggestion(@Query("message") String message);
}