package com.bubelov.coins.repository.user

import android.content.SharedPreferences
import android.os.Bundle

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.AuthResponse
import com.bubelov.coins.api.coins.NewUserParams
import com.bubelov.coins.model.User
import com.bubelov.coins.util.toStrings
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

import retrofit2.Response
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@Singleton
class UserRepository @Inject
internal constructor(
        private val api: CoinsApi,
        private val preferences: SharedPreferences,
        private val gson: Gson,
        private val analytics: FirebaseAnalytics
) {
    var user: User?
        get() = gson.fromJson(preferences.getString(PreferenceKeys.USER, null), User::class.java)
        set(user) = preferences.edit().putString(PreferenceKeys.USER, gson.toJson(user)).apply()

    var userAuthToken: String
        get() = preferences.getString(PreferenceKeys.API_AUTH_TOKEN, "")
        set(token) = preferences.edit().putString(PreferenceKeys.API_AUTH_TOKEN, token).apply()

    var userAuthMethod: String
        get() = preferences.getString(PreferenceKeys.API_AUTH_METHOD, "")
        set(method) = preferences.edit().putString(PreferenceKeys.API_AUTH_METHOD, method).apply()

    fun signIn(googleToken: String): Boolean {
        var response: Response<AuthResponse>? = null

        try {
            response = api.authWithGoogle(googleToken).execute()
        } catch (e: IOException) {
            Timber.e(e, "Couldn't authorize with Google token")
        }

        if (response == null) {
            return false
        }

        if (response.isSuccessful) {
            user = response.body()!!.user!!
            userAuthToken = response.body()!!.token!!
            userAuthMethod = "google"
            onAuthorized()
            return true
        } else {
            return false
        }
    }

    fun signIn(email: String, password: String, callback: SignInCallback) {
        api.authWithEmail(email, password).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    user = response.body()!!.user!!
                    userAuthToken = response.body()!!.token!!
                    userAuthMethod = "email"
                    onAuthorized()
                    callback.onSuccess()
                } else {
                    if (response.errorBody() != null && response.errorBody()!!.contentLength() > 0) {
                        callback.onFailure(response.errorBody()!!.toStrings())
                    } else {
                        callback.onFailure(listOf(response.message() ?: "Couldn't sign in"))
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, e: Throwable) {
                callback.onFailure(listOf(e.localizedMessage))
            }
        })
    }

    fun signUp(email: String, password: String, firstName: String, lastName: String): Boolean {
        var response: Response<AuthResponse>? = null

        try {
            response = api.createUser(NewUserParams(email, password, firstName, lastName)).execute()
        } catch (e: IOException) {
            Timber.e(e, "Couldn't sign up")
        }

        if (response == null) {
            return false
        }

        if (response.isSuccessful) {
            user = response.body()!!.user!!
            userAuthToken = response.body()!!.token!!
            userAuthMethod = "email"
            onAuthorized()
            return true
        } else {
            return false
        }
    }

    fun signedIn() = !userAuthToken.isBlank()

    fun clear() {
        user = null
        userAuthToken = ""
        userAuthMethod = ""
    }

    private fun onAuthorized() {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SIGN_UP_METHOD, userAuthMethod)
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

    interface SignInCallback {
        fun onSuccess()
        fun onFailure(errors: List<String>)
    }
}