package com.bubelov.coins.model

/**
 * @author Igor Bubelov
 */

data class Currency(
        val id: Long,
        val name: String,
        val code: String,
        val crypto: Boolean
)