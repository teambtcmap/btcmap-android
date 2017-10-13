package com.bubelov.coins.util

import com.bubelov.coins.api.coins.CoinsApiException
import retrofit2.Response

/**
 * @author Igor Bubelov
 */

fun <T> Response<T>.toCoinsApiException(): Exception {
    assert(!isSuccessful)

    val errorBodyString = errorBody()?.string()

    return if (!errorBodyString.isNullOrEmpty()) {
        CoinsApiException(errorBodyString!!)
    } else {
        CoinsApiException(message())
    }
}