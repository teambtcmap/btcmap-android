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

package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.util.AsyncResult
import javax.inject.Inject

class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    var authState: LiveData<AsyncResult<Any>> = MutableLiveData<AsyncResult<Any>>()

    fun signIn(googleToken: String): LiveData<AsyncResult<Any>> {
        authState = userRepository.signIn(googleToken)
        return authState
    }

    fun signIn(email: String, password: String): LiveData<AsyncResult<Any>> {
        authState = userRepository.signIn(email, password)
        return authState
    }

    fun signUp(email: String, password: String, firstName: String, lastName: String): LiveData<AsyncResult<Any>> {
        authState = userRepository.signUp(email, password, firstName, lastName)
        return authState
    }
}