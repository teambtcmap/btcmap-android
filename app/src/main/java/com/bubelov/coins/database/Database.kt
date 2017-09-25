package com.bubelov.coins.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

import com.bubelov.coins.database.dao.PlaceDao
import com.bubelov.coins.model.Place

/**
 * @author Igor Bubelov
 */

@Database(entities = arrayOf(Place::class), version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
}