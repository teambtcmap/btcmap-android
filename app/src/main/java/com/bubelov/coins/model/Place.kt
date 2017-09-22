package com.bubelov.coins.model

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Igor Bubelov
 */

data class Place(
        val id: Long,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val category: String,
        val description: String,
        val currencies: ArrayList<String>,
        val openedClaims: Int,
        val closedClaims: Int,
        val phone: String,
        val website: String,
        val openingHours: String,
        val visible: Boolean,
        val updatedAt: Date
) : Serializable