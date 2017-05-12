package com.bubelov.coins.data.api.coins;

import com.bubelov.coins.domain.Currency;
import com.bubelov.coins.domain.Place;
import com.bubelov.coins.domain.PlaceCategory;

import java.util.Date;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * @author Igor Bubelov
 */

public interface CoinsApi {
    @POST("users")
    Call<AuthResponse> createUser(@Body NewUserParams user);

    @POST("auth/email")
    Call<AuthResponse> authWithEmail(@Query("email") String email, @Query("password") String password);

    @POST("auth/google-token")
    Call<AuthResponse> authWithGoogle(@Header("token") String token);

    @GET("places")
    Call<List<Place>> getPlaces(@Query("since") Date since, @Query("limit") int limit);

    @POST("places")
    Call<Place> addPlace(@Header("session") String session, @Body Map<String, Object> args);

    @PATCH("places/{id}")
    Call<Place> updatePlace(@Path("id") long id, @Header("session") String session, @Body Map<String, Object> args);

    @GET("place_categories")
    Call<List<PlaceCategory>> getPlaceCategories();

    @GET("place_categories/{id}")
    Call<PlaceCategory> getPlaceCategory(@Path("id") long id);

    @GET("currencies")
    Call<List<Currency>> getCurrencies();
}