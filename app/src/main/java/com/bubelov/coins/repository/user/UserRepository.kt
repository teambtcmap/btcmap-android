package com.bubelov.coins.repository.user

import android.content.SharedPreferences

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.NewUserParams
import com.bubelov.coins.model.User
import com.bubelov.coins.util.toCoinsApiException
import com.google.gson.Gson

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class UserRepository @Inject
internal constructor(
        private val api: CoinsApi,
        private val preferences: SharedPreferences,
        private val gson: Gson
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

    fun signIn(googleToken: String) = try {
        api.authWithGoogle(googleToken).execute().apply {
            if (isSuccessful) {
                user = body()!!.user!!
                userAuthToken = body()!!.token!!
                userAuthMethod = "google"
            } else {
                throw toCoinsApiException()
            }
        }

        SignInResult.Success()
    } catch (e: Exception) {
        SignInResult.Error(e)
    }

    fun signIn(email: String, password: String) = try {
        api.authWithEmail(email, password).execute().apply {
            if (isSuccessful) {
                user = body()!!.user!!
                userAuthToken = body()!!.token!!
                userAuthMethod = "email"
            } else {
                throw toCoinsApiException()
            }
        }

        SignInResult.Success()
    } catch (e: Exception) {
        SignInResult.Error(e)
    }

    fun signUp(email: String, password: String, firstName: String, lastName: String) = try {
        api.createUser(NewUserParams(email, password, firstName, lastName)).execute().apply {
            if (isSuccessful) {
                user = body()!!.user!!
                userAuthToken = body()!!.token!!
                userAuthMethod = "email"
            } else {
                throw toCoinsApiException()
            }
        }

        SignInResult.Success()
    } catch (e: Exception) {
        SignInResult.Error(e)
    }

    fun signedIn() = !userAuthToken.isBlank()

    fun clear() {
        user = null
        userAuthToken = ""
        userAuthMethod = ""
    }
}