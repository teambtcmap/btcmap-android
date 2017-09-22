package com.bubelov.coins.api.coins

import com.bubelov.coins.model.Place

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @author Igor Bubelov
 */

interface CoinsApi {
    @POST("users")
    fun createUser(@Body user: NewUserParams): Call<AuthResponse>

    @POST("auth/email")
    fun authWithEmail(@Query("email") email: String, @Query("password") password: String): Call<AuthResponse>

    @POST("auth/google-token")
    fun authWithGoogle(@Header("token") token: String): Call<AuthResponse>

    @GET("places")
    fun getPlaces(@Query("since") since: String, @Query("limit") limit: Int): Call<List<Place>>

    @POST("places")
    fun addPlace(@Header("session") session: String, @Body place: PlaceParams): Call<Place>

    @PATCH("places/{id}")
    fun updatePlace(@Path("id") id: Long, @Header("session") session: String, @Body place: PlaceParams): Call<Place>
}