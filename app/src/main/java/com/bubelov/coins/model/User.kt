package com.bubelov.coins.model

/**
 * @author Igor Bubelov
 */

data class User(
        val id: Long,
        val email: String,
        val firstName: String,
        val lastName: String,
        val avatarUrl: String
)