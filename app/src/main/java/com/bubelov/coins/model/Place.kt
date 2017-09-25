package com.bubelov.coins.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Igor Bubelov
 */

@Entity
data class Place(
        @PrimaryKey var id: Long = 0,
        var name: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var category: String = "",
        var description: String = "",
        var currencies: ArrayList<String> = arrayListOf(),
        var openedClaims: Int = 0,
        var closedClaims: Int = 0,
        var phone: String = "",
        var website: String = "",
        var openingHours: String = "",
        var visible: Boolean = false,
        var updatedAt: Date = Date(0)
) : Serializable