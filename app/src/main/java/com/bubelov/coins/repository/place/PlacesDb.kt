/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

import com.bubelov.coins.model.Place
import java.util.*

@Dao
interface PlacesDb {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(places: List<Place>)

    @Query("SELECT * FROM Place")
    fun all(): LiveData<List<Place>>

    @Query("SELECT * from Place WHERE id = :id LIMIT 1")
    fun find(id: Long): LiveData<Place>

    @Query("SELECT * FROM Place WHERE " +
            "UPPER(name) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(category) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(description) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(phone) LIKE '%' || UPPER(:query) || '%' " +
            "OR UPPER(website) LIKE '%' || UPPER(:query) || '%'")
    fun findBySearchQuery(query: String): List<Place>

    @Query("SELECT * FROM Place ORDER BY RANDOM() LIMIT 1")
    fun findRandom(): Place?

    @Query("SELECT COUNT(*) FROM Place")
    fun count(): Int

    @Query("SELECT MAX(updatedAt) FROM Place")
    fun maxUpdatedAt(): Date?

    @Update
    fun update(place: Place)
}