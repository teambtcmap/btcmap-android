package com.bubelov.coins.repository.user

sealed class SignInResult {
    class Success : SignInResult()
    class Error(val e: Exception) : SignInResult()
}