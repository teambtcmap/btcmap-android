package com.bubelov.coins.api.coins;

import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceCategory;

import java.util.Date;
import java.util.List;

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

    @GET("places/{id}")
    Call<Place> getPlace(@Path("id") long id);

    @POST("places")
    Call<Place> addPlace(@Header("session") String session, @Body PlaceParams place);

    @PATCH("places/{id}")
    Call<Place> updatePlace(@Path("id") long id, @Header("session") String session, @Body PlaceParams place);

    @GET("place_categories")
    Call<List<PlaceCategory>> getPlaceCategories();

    @GET("place_categories/{id}")
    Call<PlaceCategory> getPlaceCategory(@Path("id") long id);

    @GET("currencies")
    Call<List<Currency>> getCurrencies();
}