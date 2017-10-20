package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

import com.bubelov.coins.model.Place
import java.util.*

/**
 * @author Igor Bubelov
 */

@Dao
interface PlacesDb {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(place: Place)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(places: List<Place>)

    @Query("SELECT * FROM places")
    fun all(): LiveData<List<Place>>

    @Query("SELECT * from places WHERE _id = :id LIMIT 1")
    fun findById(id: Long): LiveData<Place>

    @Query("SELECT * FROM places WHERE " +
            "UPPER(name) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(category) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(description) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(currencies) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(phone) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(website) LIKE '%' || UPPER(:query) || '%'")
    fun findBySearchQuery(query: String): LiveData<List<Place>>

    @Query("SELECT * FROM places ORDER BY RANDOM() LIMIT 1")
    fun random(): LiveData<Place?>

    @Query("SELECT COUNT(*) FROM places")
    fun count(): Int

    @Query("SELECT MAX(updated_at) FROM places")
    fun maxUpdatedAt(): Date?

    @Update
    fun update(place: Place)
}