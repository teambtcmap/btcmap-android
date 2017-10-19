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
        val id: Long,

        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "latitude")
        val latitude: Double,

        @ColumnInfo(name = "longitude")
        val longitude: Double,

        @ColumnInfo(name = "category")
        val category: String,

        @ColumnInfo(name = "description")
        val description: String,

        @ColumnInfo(name = "currencies")
        val currencies: ArrayList<String>,

        @ColumnInfo(name = "opened_claims")
        val openedClaims: Int,

        @ColumnInfo(name = "closed_claims")
        val closedClaims: Int,

        @ColumnInfo(name = "phone")
        val phone: String,

        @ColumnInfo(name = "website")
        val website: String,

        @ColumnInfo(name = "opening_hours")
        val openingHours: String,

        @ColumnInfo(name = "visible")
        val visible: Boolean,

        @ColumnInfo(name = "updated_at")
        val updatedAt: Date
) : Serializable