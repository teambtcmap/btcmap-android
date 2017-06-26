package com.bubelov.coins.util

import java.util.Locale

/**
 * Author: Igor Bubelov
 */

enum class DistanceUnits {
    KILOMETERS,
    MILES;

    companion object {
        val default = Locale.getDefault().toDistanceUnits()

        fun Locale.toDistanceUnits() = when(country) {
            in listOf("LR", "MM", "GB", "US") -> MILES
            else -> KILOMETERS
        }
    }
}