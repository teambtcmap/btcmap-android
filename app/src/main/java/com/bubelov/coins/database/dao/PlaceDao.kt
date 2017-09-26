package com.bubelov.coins.database.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

import com.bubelov.coins.model.Place
import java.util.*

/**
 * @author Igor Bubelov
 */

@Dao
interface PlaceDao {
    @Query("SELECT COUNT(*) FROM place")
    fun count(): Int

    @Query("SELECT MAX(updatedAt) FROM place")
    fun maxUpdatedAt(): Date?

    @Query("SELECT * FROM place")
    fun all(): LiveData<List<Place>>

    @Query("SELECT * FROM place ORDER BY RANDOM() LIMIT 1")
    fun random(): LiveData<Place?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(places: List<Place>)
}