package com.bubelov.coins.repository

sealed class ApiResult<T> {
    data class Success<T>(val places: T) : ApiResult<T>()
    data class Error<T>(val e: kotlin.Exception) : ApiResult<T>()
}