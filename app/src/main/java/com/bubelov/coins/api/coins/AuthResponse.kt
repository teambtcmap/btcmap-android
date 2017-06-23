package com.bubelov.coins.api.coins

import com.bubelov.coins.model.User

/**
 * @author Igor Bubelov
 */

data class AuthResponse (
        val user: User?,
        val token: String?,
        val errors: List<String>
)