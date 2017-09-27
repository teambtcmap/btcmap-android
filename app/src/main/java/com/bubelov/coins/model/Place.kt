package com.bubelov.coins.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Igor Bubelov
 */

@Entity(tableName = "places")
data class Place(
        @PrimaryKey
        @ColumnInfo(name = "_id")
        var id: Long = 0,

        @ColumnInfo(name = "name")
        var name: String = "",

        @ColumnInfo(name = "latitude")
        var latitude: Double = 0.0,

        @ColumnInfo(name = "longitude")
        var longitude: Double = 0.0,

        @ColumnInfo(name = "category")
        var category: String = "",

        @ColumnInfo(name = "description")
        var description: String = "",

        @ColumnInfo(name = "currencies")
        var currencies: ArrayList<String> = arrayListOf(),

        @ColumnInfo(name = "opened_claims")
        var openedClaims: Int = 0,

        @ColumnInfo(name = "closed_claims")
        var closedClaims: Int = 0,

        @ColumnInfo(name = "phone")
        var phone: String = "",

        @ColumnInfo(name = "website")
        var website: String = "",

        @ColumnInfo(name = "opening_hours")
        var openingHours: String = "",

        @ColumnInfo(name = "visible")
        var visible: Boolean = false,

        @ColumnInfo(name = "updated_at")
        var updatedAt: Date = Date(0)
) : Serializable