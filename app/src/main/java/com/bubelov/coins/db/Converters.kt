package com.bubelov.coins.db

import android.arch.persistence.room.TypeConverter
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Igor Bubelov
 */

class Converters {
    @TypeConverter
    fun longToDate(long: Long) = Date(long)

    @TypeConverter
    fun dateToLong(date: Date) = date.time

    @TypeConverter
    fun stringToArrayList(string: String) = Gson().fromJson(string, arrayListOf<String>().javaClass)!!

    @TypeConverter
    fun arrayListToString(arrayList: ArrayList<String>) = Gson().toJson(arrayList)!!
}