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

package com.bubelov.coins.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration

import com.bubelov.coins.repository.place.PlacesDb
import com.bubelov.coins.model.Place
import com.bubelov.coins.model.Preference
import com.bubelov.coins.repository.preference.PreferencesDb
import com.bubelov.coins.util.transaction

@Database(entities = [Place::class, Preference::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun placesDb(): PlacesDb
    abstract fun preferencesDb(): PreferencesDb

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.transaction {
                    database.execSQL("DROP TABLE IF EXISTS places")

                    database.execSQL(
                        "CREATE TABLE `Place` (" +
                                "`id` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`latitude` REAL NOT NULL, " +
                                "`longitude` REAL NOT NULL, " +
                                "`category` TEXT NOT NULL, " +
                                "`description` TEXT NOT NULL, " +
                                "`currencies` TEXT NOT NULL, " +
                                "`openedClaims` INTEGER NOT NULL, " +
                                "`closedClaims` INTEGER NOT NULL, " +
                                "`phone` TEXT NOT NULL, " +
                                "`website` TEXT NOT NULL, " +
                                "`openingHours` TEXT NOT NULL, " +
                                "`visible` INTEGER NOT NULL, " +
                                "`updatedAt` INTEGER NOT NULL, " +
                                "PRIMARY KEY(`id`)" +
                                ")"
                    )

                    database.execSQL(
                        "CREATE TABLE `Preference` (" +
                                "`key` TEXT NOT NULL, " +
                                "`value` TEXT NOT NULL, " +
                                "PRIMARY KEY(`key`)" +
                                ")"
                    )
                }
            }
        }
    }
}