package com.bubelov.coins.model

import java.io.Serializable
import java.util.*

/**
 * @author Igor Bubelov
 */

data class Place(
        val id: Long,
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val categoryId: Long,
        val phone: String,
        val website: String,
        val openingHours: String,
        val visible: Boolean,
        val updatedAt: Date
) : Serializable