package com.bubelov.coins.util

import com.google.gson.Gson
import okhttp3.ResponseBody

/**
 * @author Igor Bubelov
 */

fun ResponseBody.toStrings(): List<String> {
    return Gson().fromJson<List<String>>(charStream(), mutableListOf<String>().javaClass)
}