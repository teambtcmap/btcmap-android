/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.repository.user

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.CreateUserArgs
import com.bubelov.coins.model.User
import com.bubelov.coins.util.AsyncResult
import com.bubelov.coins.util.getException
import com.google.gson.Gson
import kotlinx.coroutines.experimental.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
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

    fun signIn(googleToken: String): LiveData<AsyncResult<Any>> {
        val result = MutableLiveData<AsyncResult<Any>>()
        result.postValue(AsyncResult.Loading)

        launch {
            try {
                api.authWithGoogle(googleToken).execute().apply {
                    if (isSuccessful) {
                        val body = body() ?: throw getException()
                        user = body.user
                        userAuthToken = body.token
                        userAuthMethod = "google"
                        result.postValue(AsyncResult.Success(Any()))
                    } else {
                        throw getException()
                    }
                }
            } catch (t: Throwable) {
                result.postValue(AsyncResult.Error(t))
            }
        }

        return result
    }

    fun signIn(email: String, password: String): LiveData<AsyncResult<Any>> {
        val result = MutableLiveData<AsyncResult<Any>>()
        result.postValue(AsyncResult.Loading)

        launch {
            try {
                api.authWithEmail(email, password).execute().apply {
                    if (isSuccessful) {
                        val body = body() ?: throw getException()
                        user = body.user
                        userAuthToken = body.token
                        userAuthMethod = "email"
                    } else {
                        throw getException()
                    }
                }

                result.postValue(AsyncResult.Success(Any()))
            } catch (t: Throwable) {
                result.postValue(AsyncResult.Error(t))
            }
        }

        return result
    }

    fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): LiveData<AsyncResult<Any>> {
        val result = MutableLiveData<AsyncResult<Any>>()
        result.postValue(AsyncResult.Loading)

        launch {
            try {
                val args = CreateUserArgs(
                    CreateUserArgs.User(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName
                    )
                )

                api.createUser(args)
                    .execute().apply {
                        if (isSuccessful) {
                            val body = body() ?: throw getException()
                            user = body.user
                            userAuthToken = body.token
                            userAuthMethod = "email"
                        } else {
                            throw getException()
                        }
                    }
            } catch (t: Throwable) {
                result.postValue(AsyncResult.Error(t))
            }
        }

        return result
    }

    fun signedIn() = !userAuthToken.isBlank()

    fun clear() {
        user = null
        userAuthToken = ""
        userAuthMethod = ""
    }
}